-- Closing the day at the front desk (docs/DAILY_CASH_SUMMARY_PLAN.md): the cash counted against what the bills say
-- the drawer should hold since the previous close. A close is never changed. The period's start is unique, so two
-- closes made at the same moment can't both cover the same cash.

CREATE TABLE `cash_closes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `period_start` datetime(6) NOT NULL,
  `closed_at` datetime(6) NOT NULL,
  `closed_by` varchar(100) NOT NULL,
  `cash_in` decimal(10,2) NOT NULL,
  `cash_out` decimal(10,2) NOT NULL,
  `cash_expected` decimal(10,2) NOT NULL,
  `cash_counted` decimal(10,2) NOT NULL,
  `cash_difference` decimal(10,2) NOT NULL,
  `note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cash_closes_period_start` (`period_start`),
  KEY `idx_cash_closes_closed_at` (`closed_at`)
) ENGINE=InnoDB;

-- The summary finds a day's payments and refunds by when they were made
CREATE INDEX `idx_bills_paid_at` ON `bills` (`paid_at`);
CREATE INDEX `idx_bills_refunded_at` ON `bills` (`refunded_at`);
