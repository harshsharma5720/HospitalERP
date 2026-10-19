package com.itmonteur.hospitalerp.identity.internal;

/** Why a login session ended (docs/SAFER_LOGIN_TOKENS_PLAN.md). */
public enum SessionEndReason {
    LOGOUT, LOGOUT_ALL, REUSED, PASSWORD_RESET, DEACTIVATED
}
