-- Online payment with Razorpay (docs/ONLINE_PAYMENT_PLAN.md): one row per Razorpay order made for a bill, with the
-- payment that paid it, or the refund when the payment couldn't be applied (the bill was paid, changed or cancelled
-- meanwhile). A bill paid online keeps the payment on the bill itself (method ONLINE, the payment id as reference).

CREATE TABLE `online_payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_id` bigint NOT NULL,
  `order_id` varchar(40) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(13) NOT NULL,
  `payment_id` varchar(40) DEFAULT NULL,
  `refund_id` varchar(40) DEFAULT NULL,
  `note` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_online_payments_order` (`order_id`),
  KEY `idx_online_payments_bill` (`bill_id`),
  CONSTRAINT `fk_online_payments_bill` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`)
) ENGINE=InnoDB;
