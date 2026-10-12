import { render, screen, fireEvent, act } from "@testing-library/react";
import { MemoryRouter, Link } from "react-router-dom";
import {
  Button, Card, CardHeader, PageHeader, Field, Input, Select, Badge, Table, Th, Td, TableMessage, Pagination, Notice,
  EmptyState, Modal, ToastProvider, useToast, ConfirmProvider, useConfirm,
} from "./index";
import { useState } from "react";

describe("Button", () => {
  test("is a plain button unless told otherwise, so it never submits a form by accident", () => {
    const onSubmit = jest.fn((e) => e.preventDefault());
    render(
      <form onSubmit={onSubmit}>
        <Button>Add</Button>
        <Button type="submit">Save</Button>
      </form>
    );
    expect(screen.getByRole("button", { name: "Add" })).toHaveAttribute("type", "button");
    fireEvent.click(screen.getByRole("button", { name: "Add" }));
    expect(onSubmit).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  test("looks like its variant and can't be clicked while disabled", () => {
    const onClick = jest.fn();
    render(
      <>
        <Button variant="danger" onClick={onClick} disabled>
          Refund
        </Button>
        <Button variant="secondary">Back</Button>
      </>
    );
    const refund = screen.getByRole("button", { name: "Refund" });
    expect(refund).toHaveClass("bg-danger");
    fireEvent.click(refund);
    expect(onClick).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Back" })).toHaveClass("bg-surface", "border-line");
  });

  test("as a link it goes somewhere and has no button type", () => {
    render(
      <MemoryRouter>
        <Button as={Link} to="/bills/7">
          Open bill
        </Button>
      </MemoryRouter>
    );
    const link = screen.getByRole("link", { name: "Open bill" });
    expect(link).toHaveAttribute("href", "/bills/7");
    expect(link).not.toHaveAttribute("type");
    expect(link).toHaveClass("bg-primary");
  });
});

describe("page parts", () => {
  test("a page header shows its title, description, back link and actions", () => {
    render(
      <PageHeader title="Bills" description="Today's bills" back={<a href="/x">Back to patients</a>} actions={<Button>New</Button>} />
    );
    expect(screen.getByRole("heading", { level: 1, name: "Bills" })).toBeInTheDocument();
    expect(screen.getByText("Today's bills")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to patients" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "New" })).toBeInTheDocument();
  });

  test("a card header's heading can name its card", () => {
    render(
      <Card aria-labelledby="visits">
        <CardHeader id="visits" title="Visits" description="Latest first" />
      </Card>
    );
    expect(screen.getByRole("region", { name: "Visits" })).toHaveTextContent("Latest first");
  });

  test("a field's label points at its control and an error replaces the hint", () => {
    const { rerender } = render(
      <Field label="Phone" htmlFor="phone" hint="10 digits">
        <Input id="phone" />
      </Field>
    );
    expect(screen.getByLabelText("Phone")).toHaveAttribute("id", "phone");
    expect(screen.getByText("10 digits")).toBeInTheDocument();

    rerender(
      <Field label="Phone" htmlFor="phone" hint="10 digits" error="Phone is required">
        <Input id="phone" />
      </Field>
    );
    expect(screen.getByText("Phone is required")).toBeInTheDocument();
    expect(screen.queryByText("10 digits")).not.toBeInTheDocument();
  });

  test("a select is labelled the same way", () => {
    render(
      <Field label="Doctor" htmlFor="doctor">
        <Select id="doctor" defaultValue="2">
          <option value="1">Dr. Rao</option>
          <option value="2">Dr. Iyer</option>
        </Select>
      </Field>
    );
    expect(screen.getByLabelText("Doctor")).toHaveValue("2");
  });

  test("a badge says its label in its tone", () => {
    render(<Badge tone="success">Paid</Badge>);
    expect(screen.getByText("Paid")).toHaveClass("text-success-ink");
  });

  test("a table has header cells and a message row across the table", () => {
    render(
      <Table>
        <thead>
          <tr>
            <Th>Patient</Th>
            <Th align="right">Total</Th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <Td>Asha</Td>
            <Td align="right">Rs. 500.00</Td>
          </tr>
          <TableMessage colSpan={2}>No more bills.</TableMessage>
        </tbody>
      </Table>
    );
    expect(screen.getByRole("columnheader", { name: "Total" })).toHaveClass("text-right");
    expect(screen.getByRole("cell", { name: "No more bills." })).toHaveAttribute("colspan", "2");
  });

  test("an empty state shows its title, help and action", () => {
    render(<EmptyState title="No patients yet" action={<Button>Register</Button>}>Register a walk-in to start.</EmptyState>);
    expect(screen.getByText("No patients yet")).toBeInTheDocument();
    expect(screen.getByText("Register a walk-in to start.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Register" })).toBeInTheDocument();
  });
});

describe("Pagination", () => {
  test("says where we are and moves a page back or forward", () => {
    const onPage = jest.fn();
    render(<Pagination page={1} totalPages={5} totalEntries={93} noun="patients" onPage={onPage} />);
    expect(screen.getByText("Page 2 of 5 · 93 patients")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Previous" }));
    expect(onPage).toHaveBeenLastCalledWith(0);
    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(onPage).toHaveBeenLastCalledWith(2);
  });

  test("can't go before the first page or after the last", () => {
    const { rerender } = render(<Pagination page={0} totalPages={2} totalEntries={30} noun="bills" onPage={() => {}} />);
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Next" })).toBeEnabled();
    rerender(<Pagination page={1} totalPages={2} totalEntries={30} noun="bills" onPage={() => {}} />);
    expect(screen.getByRole("button", { name: "Previous" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  test("shows an empty list as page 0 of 0 and waits while loading", () => {
    const { rerender } = render(<Pagination page={0} totalPages={0} totalEntries={0} noun="bills" onPage={() => {}} />);
    expect(screen.getByText("Page 0 of 0 · 0 bills")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
    rerender(<Pagination page={1} totalPages={3} totalEntries={50} noun="bills" onPage={() => {}} loading />);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });
});

describe("Notice", () => {
  test("an error is an alert, anything else a status, and nothing shows nothing", () => {
    const { rerender, container } = render(<Notice tone="danger">Could not save.</Notice>);
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save.");
    rerender(<Notice tone="success">Saved.</Notice>);
    expect(screen.getByRole("status")).toHaveTextContent("Saved.");
    rerender(<Notice tone="success">{""}</Notice>);
    expect(container).toBeEmptyDOMElement();
  });
});

function ToastButtons() {
  const toast = useToast();
  return (
    <>
      <Button onClick={() => toast.success("Fee saved.")}>ok</Button>
      <Button onClick={() => toast.error("Could not save the fee.")}>fail</Button>
    </>
  );
}

describe("toasts", () => {
  afterEach(() => jest.useRealTimers());

  test("a success toast shows, and goes away by itself", () => {
    jest.useFakeTimers();
    render(
      <ToastProvider duration={1000}>
        <ToastButtons />
      </ToastProvider>
    );
    fireEvent.click(screen.getByRole("button", { name: "ok" }));
    expect(screen.getByRole("status")).toHaveTextContent("Fee saved.");
    act(() => jest.advanceTimersByTime(999));
    expect(screen.getByText("Fee saved.")).toBeInTheDocument();
    act(() => jest.advanceTimersByTime(1));
    expect(screen.queryByText("Fee saved.")).not.toBeInTheDocument();
  });

  test("an error toast is an alert and stays twice as long", () => {
    jest.useFakeTimers();
    render(
      <ToastProvider duration={1000}>
        <ToastButtons />
      </ToastProvider>
    );
    fireEvent.click(screen.getByRole("button", { name: "fail" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Could not save the fee.");
    act(() => jest.advanceTimersByTime(1999));
    expect(screen.getByRole("alert")).toBeInTheDocument();
    act(() => jest.advanceTimersByTime(1));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  test("× closes a toast at once, leaving the others", () => {
    render(
      <ToastProvider>
        <ToastButtons />
      </ToastProvider>
    );
    fireEvent.click(screen.getByRole("button", { name: "ok" }));
    fireEvent.click(screen.getByRole("button", { name: "fail" }));
    const [closeFirst] = screen.getAllByRole("button", { name: "Close" });
    fireEvent.click(closeFirst);
    expect(screen.queryByText("Fee saved.")).not.toBeInTheDocument();
    expect(screen.getByText("Could not save the fee.")).toBeInTheDocument();
  });

  test("without a provider a toast does nothing", () => {
    render(<ToastButtons />);
    fireEvent.click(screen.getByRole("button", { name: "ok" }));
    expect(screen.queryByText("Fee saved.")).not.toBeInTheDocument();
  });
});

function DeactivateButton({ onAnswer }) {
  const confirm = useConfirm();
  return (
    <Button
      onClick={async () =>
        onAnswer(await confirm({ title: "Deactivate Asha?", message: "She can't log in any more.", confirmLabel: "Deactivate", tone: "danger" }))
      }
    >
      Deactivate user
    </Button>
  );
}

const renderConfirm = () => {
  const onAnswer = jest.fn();
  render(
    <ConfirmProvider>
      <DeactivateButton onAnswer={onAnswer} />
    </ConfirmProvider>
  );
  fireEvent.click(screen.getByRole("button", { name: "Deactivate user" }));
  return onAnswer;
};

describe("confirm dialog", () => {
  test("asks, with the confirm button focused, and answers yes", async () => {
    const onAnswer = renderConfirm();
    const dialog = screen.getByRole("dialog", { name: "Deactivate Asha?" });
    expect(dialog).toHaveAccessibleDescription("She can't log in any more.");
    const deactivate = screen.getByRole("button", { name: "Deactivate" });
    expect(deactivate).toHaveFocus();
    expect(deactivate).toHaveClass("bg-danger");
    await act(async () => fireEvent.click(deactivate));
    expect(onAnswer).toHaveBeenCalledWith(true);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("Cancel answers no", async () => {
    const onAnswer = renderConfirm();
    await act(async () => fireEvent.click(screen.getByRole("button", { name: "Cancel" })));
    expect(onAnswer).toHaveBeenCalledWith(false);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("Escape answers no", async () => {
    const onAnswer = renderConfirm();
    await act(async () => fireEvent.keyDown(document, { key: "Escape" }));
    expect(onAnswer).toHaveBeenCalledWith(false);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("a click beside the dialog answers no, a click inside it doesn't", async () => {
    const onAnswer = renderConfirm();
    fireEvent.click(screen.getByRole("dialog"));
    expect(onAnswer).not.toHaveBeenCalled();
    await act(async () => fireEvent.click(screen.getByRole("dialog").parentElement));
    expect(onAnswer).toHaveBeenCalledWith(false);
  });

  test("without a provider it asks the browser", async () => {
    const browserConfirm = jest.spyOn(window, "confirm").mockReturnValue(true);
    const onAnswer = jest.fn();
    render(<DeactivateButton onAnswer={onAnswer} />);
    await act(async () => fireEvent.click(screen.getByRole("button", { name: "Deactivate user" })));
    expect(browserConfirm).toHaveBeenCalledWith("Deactivate Asha?\n\nShe can't log in any more.");
    expect(onAnswer).toHaveBeenCalledWith(true);
    browserConfirm.mockRestore();
  });
});

function LeaveForm({ onClose, closeOnBackdrop }) {
  const [reason, setReason] = useState("");
  return (
    <Modal title="Apply for leave" description="Pending until an admin approves it." onClose={onClose} closeOnBackdrop={closeOnBackdrop}>
      <Field label="From" htmlFor="m-from">
        <Input id="m-from" type="date" />
      </Field>
      <Field label="Reason" htmlFor="m-reason">
        <Input id="m-reason" value={reason} onChange={(e) => setReason(e.target.value)} />
      </Field>
    </Modal>
  );
}

describe("modal", () => {
  test("is a named dialog that opens on its first field and keeps the page from scrolling", () => {
    const { unmount } = render(<LeaveForm onClose={() => {}} />);
    const dialog = screen.getByRole("dialog", { name: "Apply for leave" });
    expect(dialog).toHaveAccessibleDescription("Pending until an admin approves it.");
    expect(screen.getByLabelText("From")).toHaveFocus();
    expect(document.body.style.overflow).toBe("hidden");
    unmount();
    expect(document.body.style.overflow).toBe("");
  });

  test("typing in a later field keeps the focus there", () => {
    render(<LeaveForm onClose={() => {}} />);
    const reason = screen.getByLabelText("Reason");
    reason.focus();
    fireEvent.change(reason, { target: { value: "Conference" } });
    expect(reason).toHaveValue("Conference");
    expect(reason).toHaveFocus();
  });

  test("closes with ×, with Escape and with a click beside it", () => {
    const onClose = jest.fn();
    render(<LeaveForm onClose={onClose} />);
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    fireEvent.keyDown(document, { key: "Escape" });
    fireEvent.mouseDown(screen.getByRole("dialog"));
    expect(onClose).toHaveBeenCalledTimes(2);
    fireEvent.mouseDown(screen.getByRole("dialog").parentElement);
    expect(onClose).toHaveBeenCalledTimes(3);
  });

  test("a form that shouldn't be lost ignores a click beside it", () => {
    const onClose = jest.fn();
    render(<LeaveForm onClose={onClose} closeOnBackdrop={false} />);
    fireEvent.mouseDown(screen.getByRole("dialog").parentElement);
    expect(onClose).not.toHaveBeenCalled();
  });
});
