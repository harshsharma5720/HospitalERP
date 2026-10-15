// Loading Razorpay Checkout's script (docs/ONLINE_PAYMENT_PLAN.md): once, and again after a failed load
const script = () => document.querySelectorAll('script[src="https://checkout.razorpay.com/v1/checkout.js"]');

beforeEach(() => {
  jest.resetModules();
  delete window.Razorpay;
  script().forEach((s) => s.remove());
});

test("the script is added once, however often it is asked for", async () => {
  const { loadCheckout } = require("./razorpayCheckout");
  const first = loadCheckout();
  const second = loadCheckout();
  expect(script()).toHaveLength(1);
  window.Razorpay = function Razorpay() {};
  script()[0].onload();
  await expect(first).resolves.toBe(window.Razorpay);
  await expect(second).resolves.toBe(window.Razorpay);
  await expect(loadCheckout()).resolves.toBe(window.Razorpay);
  expect(script()).toHaveLength(1);
});

test("a script that fails to load is tried again next time", async () => {
  const { loadCheckout } = require("./razorpayCheckout");
  const failed = loadCheckout();
  script()[0].onerror();
  await expect(failed).rejects.toThrow("Checkout couldn't be loaded");
  expect(script()).toHaveLength(0);
  loadCheckout();
  expect(script()).toHaveLength(1);
});
