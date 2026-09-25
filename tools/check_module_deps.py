#!/usr/bin/env python3
"""
Module dependency checker for the multi-module refactor (docs/MULTI_MODULE_PLAN.md).

Assigns every backend class to a target module, finds which modules each class uses,
and reports:
  * dependencies that are not allowed by the plan (section 2.1),
  * dependency cycles between modules,
  * hidden dependencies inside @Query strings,
  * classes that are not assigned to a module yet.

Exit code 0 = clean, 1 = violations found.

Usage (from the repository root):
    python tools/check_module_deps.py            # summary + violations
    python tools/check_module_deps.py --verbose  # also list every module-to-module edge

Module of a class:
  1. once code lives in com.itmonteur.hospitalerp.<module>...  -> taken from the package (after Phase 2)
  2. otherwise                                                 -> looked up in CLASS_MODULE below
When you add a class during Phase 1, add it to CLASS_MODULE.
"""
import collections
import os
import re
import sys

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'hospitalERP', 'src', 'main', 'java')
TARGET_ROOT_PACKAGE = 'com.itmonteur.hospitalerp'

# Plan section 2.1: which modules each module may use.
ALLOWED = {
    'common': set(),
    'notifications': {'common'},
    'identity': {'common', 'notifications'},
    'patients': {'common', 'identity'},
    'staff': {'common', 'identity'},
    'scheduling': {'common', 'identity', 'staff'},
    'appointments': {'common', 'identity', 'notifications', 'patients', 'staff', 'scheduling'},
    'clinical': {'common', 'identity', 'patients', 'staff', 'appointments'},
    'administration': {'common', 'notifications', 'identity', 'patients', 'staff', 'scheduling',
                       'appointments', 'clinical'},
    'app': {'common', 'notifications', 'identity', 'patients', 'staff', 'scheduling', 'appointments',
            'clinical', 'administration'},
}

# Plan section 3: current class -> target module.
CLASS_MODULE = {
    # common
    'ApiResponse': 'common', 'BadRequestException': 'common', 'ConflictException': 'common',
    'ForbiddenException': 'common', 'ResourceNotFoundException': 'common', 'TooManyRequestsException': 'common',
    'GlobalExceptionHandler': 'common', 'Gender': 'common', 'FileStorageService': 'common', 'WebConfig': 'common',
    # notifications
    'EmailService': 'notifications', 'SmsService': 'notifications', 'NotificationService': 'notifications',
    'TwilioConfig': 'notifications',
    # identity
    'User': 'identity', 'Role': 'identity', 'UserRepository': 'identity', 'UserDTO': 'identity',
    'AuthService': 'identity', 'AuthController': 'identity', 'JWTService': 'identity',
    'JWTAuthenticationFilter': 'identity', 'CustomUserDetailsService': 'identity', 'CurrentUserService': 'identity',
    'OtpService': 'identity', 'LoginAttemptService': 'identity', 'PasswordResetService': 'identity',
    'AdminBootstrap': 'identity', 'AuthResponseDTO': 'identity', 'LoginRequestDTO': 'identity',
    'RegisterRequestDTO': 'identity', 'ForgotPasswordRequestDTO': 'identity', 'ResetPasswordRequestDTO': 'identity',
    # patients
    'PtInfo': 'patients', 'PtRelative': 'patients', 'RelationShip': 'patients', 'PtInfoRepository': 'patients',
    'PtRelativeRepository': 'patients', 'PtInfoService': 'patients', 'PtRelativeService': 'patients',
    'PtInfoController': 'patients', 'PtRelativeController': 'patients', 'PtInfoDTO': 'patients',
    'PtRelativeDTO': 'patients',
    # staff
    'Doctor': 'staff', 'Receptionist': 'staff', 'Specialist': 'staff', 'LeaveRequest': 'staff',
    'LeaveStatus': 'staff', 'DoctorRepository': 'staff', 'ReceptionistRepository': 'staff',
    'LeaveRequestRepository': 'staff', 'DoctorService': 'staff', 'ReceptionistService': 'staff',
    'LeaveRequestService': 'staff', 'DoctorController': 'staff', 'ReceptionistController': 'staff',
    'LeaveRequestController': 'staff', 'DoctorDTO': 'staff', 'ReceptionistDTO': 'staff', 'LeaveRequestDTO': 'staff',
    # scheduling
    'Slot': 'scheduling', 'Shift': 'scheduling', 'DoctorSchedule': 'scheduling', 'SlotRepository': 'scheduling',
    'DoctorScheduleRepository': 'scheduling', 'SlotService': 'scheduling', 'DoctorScheduleService': 'scheduling',
    'ScheduleDefaults': 'scheduling', 'SlotController': 'scheduling', 'DoctorScheduleDTO': 'scheduling',
    # appointments
    'Appointment': 'appointments', 'AppointmentStatus': 'appointments', 'AppointmentRepository': 'appointments',
    'AppointmentService': 'appointments', 'AppointmentReminderService': 'appointments',
    'AppointmentController': 'appointments', 'AppointmentDTO': 'appointments',
    # clinical
    'Consultation': 'clinical', 'PrescriptionItem': 'clinical', 'ConsultationRepository': 'clinical',
    'ConsultationService': 'clinical', 'PrescriptionPdfService': 'clinical', 'ConsultationController': 'clinical',
    'ConsultationDTO': 'clinical', 'PrescriptionItemDTO': 'clinical',
    # administration
    'AdminController': 'administration', 'AdminService': 'administration', 'UserAccountService': 'administration',
    # app
    'HospitalErpApplication': 'app', 'SecurityConfig': 'app',
    # added in step 1.1 (replace the former multi-module EntityMapper)
    'AppointmentMapper': 'appointments', 'DoctorMapper': 'staff', 'PatientMapper': 'patients',
    # added in step 1.2 (registration via event)
    'UserRegisteredEvent': 'identity', 'PatientProfileCreator': 'patients', 'StaffProfileCreator': 'staff',
    # added in step 1.4 (relative deletion via event)
    'RelativeDeletedEvent': 'patients', 'AppointmentRelativeUnlinker': 'appointments',
    # added in step 1.5 (leave approval via event)
    'DoctorLeaveApprovedEvent': 'staff', 'SlotLeaveBlocker': 'scheduling', 'AppointmentLeaveCanceller': 'appointments',
    # added in step 1.6 (endpoints moved to the module that owns them; URLs unchanged)
    'DoctorAppointmentController': 'appointments', 'ReceptionistAppointmentController': 'appointments',
    'DoctorScheduleController': 'scheduling', 'DoctorDirectoryController': 'staff',
    # added in step 1.7 (account deletion only in administration)
    'AccountController': 'administration',
}


def strip_comments(src):
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    return re.sub(r'//[^\n]*', '', src)


def main():
    verbose = '--verbose' in sys.argv
    classes = {}  # simple name -> (path, module)
    unmapped = []
    for dirpath, _, filenames in os.walk(SRC):
        for filename in filenames:
            if not filename.endswith('.java'):
                continue
            name = filename[:-5]
            path = os.path.join(dirpath, filename)
            src = open(path, encoding='utf-8').read()
            pkg = re.search(r'^\s*package\s+([\w.]+)\s*;', src, flags=re.M)
            module = None
            if pkg and pkg.group(1).startswith(TARGET_ROOT_PACKAGE + '.'):
                module = pkg.group(1)[len(TARGET_ROOT_PACKAGE) + 1:].split('.')[0]
            elif pkg and pkg.group(1) == TARGET_ROOT_PACKAGE:
                module = 'app'
            else:
                module = CLASS_MODULE.get(name)
            if module is None:
                unmapped.append(name)
                module = '<unmapped>'
            classes[name] = (path, module)

    names = sorted(classes, key=len, reverse=True)
    edges = collections.defaultdict(set)   # (from, to) -> {"A -> B"}
    hidden = collections.defaultdict(set)
    for name, (path, module) in classes.items():
        src = strip_comments(open(path, encoding='utf-8').read())
        query_strings = ' '.join(re.findall(r'@Query\s*\((.*?)\)\s*\n', src, flags=re.S))
        code = re.sub(r'"(?:[^"\\]|\\.)*"', '""', src)
        for other in names:
            if other == name:
                continue
            other_module = classes[other][1]
            if other_module == module:
                continue
            if re.search(r'\b' + other + r'\b', code):
                edges[(module, other_module)].add(f'{name} -> {other}')
            elif re.search(r'\b' + other + r'\b', query_strings):
                hidden[(module, other_module)].add(f'{name} -> {other} (inside @Query)')

    violations = []
    for (frm, to), examples in sorted(edges.items()):
        if frm.startswith('<') or to.startswith('<'):
            violations.append((frm, to, examples, 'class not assigned to a real module'))
        elif to not in ALLOWED.get(frm, set()):
            violations.append((frm, to, examples, 'not allowed'))
    for (frm, to), examples in sorted(hidden.items()):
        if not frm.startswith('<') and to not in ALLOWED.get(frm, set()):
            violations.append((frm, to, examples, 'hidden, not allowed'))

    # cycles (strongly connected components of the module graph)
    graph = collections.defaultdict(set)
    for (frm, to) in edges:
        graph[frm].add(to)
    cycles = find_cycles(graph)

    print(f'Classes scanned: {len(classes)}   modules: {len({m for _, m in classes.values()})}')
    if unmapped:
        print(f'\nUNMAPPED classes (add them to CLASS_MODULE): {", ".join(sorted(unmapped))}')
    if verbose:
        print('\nAll module edges:')
        for (frm, to), examples in sorted(edges.items()):
            print(f'  {frm:15s} -> {to:15s} ({len(examples)} refs)')
    print(f'\nDisallowed dependencies: {len(violations)}')
    for frm, to, examples, why in violations:
        shown = sorted(examples)
        more = f' (+{len(shown) - 5} more)' if len(shown) > 5 else ''
        print(f'  {frm} -> {to}  [{why}]')
        print(f'      {"; ".join(shown[:5])}{more}')
    print(f'\nCycle groups (modules that all depend on each other): {len(cycles)}')
    for cycle in cycles:
        print('  ' + ' <-> '.join(cycle))
    pairs = sorted((a, b) for (a, b) in edges if a < b and (b, a) in edges)
    print(f'\nTwo-way dependencies between module pairs: {len(pairs)}')
    for a, b in pairs:
        print(f'  {a} <-> {b}')
        print(f'      {a} -> {b}: {"; ".join(sorted(edges[(a, b)])[:3])}')
        print(f'      {b} -> {a}: {"; ".join(sorted(edges[(b, a)])[:3])}')

    ok = not violations and not cycles and not unmapped
    print('\nRESULT: ' + ('OK - module boundaries respected' if ok else 'VIOLATIONS FOUND'))
    return 0 if ok else 1


def find_cycles(graph):
    """Returns module groups that depend on each other (Tarjan's strongly connected components)."""
    index, low, stack, on_stack, result = {}, {}, [], set(), []
    counter = [0]

    def visit(node):
        index[node] = low[node] = counter[0]
        counter[0] += 1
        stack.append(node)
        on_stack.add(node)
        for nxt in graph.get(node, ()):
            if nxt not in index:
                visit(nxt)
                low[node] = min(low[node], low[nxt])
            elif nxt in on_stack:
                low[node] = min(low[node], index[nxt])
        if low[node] == index[node]:
            component = []
            while True:
                item = stack.pop()
                on_stack.discard(item)
                component.append(item)
                if item == node:
                    break
            if len(component) > 1:
                result.append(sorted(component))

    for node in list(graph):
        if node not in index:
            visit(node)
    return result


if __name__ == '__main__':
    sys.exit(main())
