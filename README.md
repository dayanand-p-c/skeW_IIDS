# skeW_IIDS
SKew is an inventory intelligence system designed to identify potential stock imbalances using inventory levels, demand patterns, and expiry risks. It combines configurable rule based risk scoring with dashboards and alerts to help make inventory decisions more informed.

## Problem

FMCG products move in high volume on thin margins, and many categories (dairy, beverages, bakery, packaged foods) carry short shelf lives. Stockouts lose sales, overstock ties up working capital, and stock that expires or falls below a retailer's minimum shelf-life requirement is written off. Spreadsheet tracking reacts too slowly to this, and full ERP suites are heavy for a single distribution point or store cluster. This project sits in the middle: a lightweight, rule-based tracker with batch-level visibility.

## Aim

Track FMCG stock by SKU and batch, flag low-stock and near-expiry risk early, and move inventory control from reactive to proactive.

## Objectives

1. Define a cloud-ready data model for SKUs, batches, locations and stock movements.
2. Implement rule-based detection: reorder-point thresholds and expiry-window checks.
3. Define a composite risk score per SKU/batch from stock level, remaining shelf life and demand variability.
4. Validate the system on synthetic transaction data.

## Scope

| In scope | Out of scope (current phase) |
|---|---|
| Rule-based tracking at SKU/batch level | ML demand forecasting |
| Low-stock and near-expiry alerts | Multi-company and supplier integration |
| Tier-wise view: plant, depot, distributor, retail | Procurement automation |
| Consolidated dashboard | Real sales or retailer data |
| Synthetic data | Regulatory and compliance tracking |

---

## System design

**Layers**

```
Data layer        SKU, batch, location and movement records
Detection layer   low-stock check, near-expiry check, severity scoring
Output layer      single consolidated, severity-coded alert view
```

**Implemented prototype:** SQLite, Python with Pandas, Streamlit dashboard.

**Proposed production design:** cloud-hosted MySQL, Spring Boot service exposing inventory and alert APIs, Tableau for analytics, email/SMS alerting, role-based access control. The prototype validates the detection logic before it is ported to this stack.

## Data model

| Table | Purpose |
|---|---|
| `skus` | Product master: SKU code, name, category, brand, pack unit |
| `batches` | Batch code, manufacture date, expiry date, unit cost |
| `locations` | Supply chain nodes by tier (Plant, Depot, Distributor, Retail) and region |
| `stock` | On-hand quantity per batch per location, with reorder level |
| `stock_movements` | Inbound, outbound and adjustment log for traceability |
| `alerts` | Persisted alert history with status |

## Detection logic

**Low stock** is flagged when `quantity < reorder_level`. Severity scales with the shortfall.

| Shortfall below reorder level | Severity |
|---|---|
| 90% or more | CRITICAL |
| 60% to 89% | HIGH |
| 30% to 59% | MEDIUM |
| under 30% | LOW |

**Near expiry** is flagged when days to expiry falls inside a configurable window (default 90 days). Expired stock is always CRITICAL.

| Days to expiry | Severity |
|---|---|
| already expired | CRITICAL |
| 0 to 15 | HIGH |
| 16 to 45 | MEDIUM |
| 46 to 90 | LOW |

Reorder levels and the expiry window are configurable per run.

---

## Repository structure

```
fmcg-inventory-tracker/
├── README.md
├── requirements.txt
├── config/
│   └── thresholds.yaml          reorder levels, expiry window, severity cut-offs
├── db/
│   └── schema.sql               table definitions
├── src/
│   └── tracker/
│       ├── database.py          connection handling and shared queries
│       ├── data_generator.py    synthetic SKU, batch and stock generation
│       ├── detection/
│       │   ├── low_stock.py     reorder-point check
│       │   ├── near_expiry.py   expiry-window check
│       │   └── severity.py      severity scoring rules
│       ├── services/
│       │   └── alerts.py        consolidates and persists alerts
│       └── dashboard/
│           └── app.py           Streamlit dashboard
├── scripts/
│   ├── seed_database.py         build the DB and load synthetic data
│   └── run_detection.py         run checks from the console
├── tests/                       unit tests for detection rules
├── data/                        generated SQLite database (git-ignored)
└── docs/                        architecture notes
```

## Setup

```bash
pip install -r requirements.txt

python scripts/seed_database.py          # build DB, load synthetic data
python scripts/run_detection.py          # print flagged items
streamlit run src/tracker/dashboard/app.py
```

The dashboard opens at `http://localhost:8501` with summary counts, a colour-coded alert table, filters by tier, category and severity, breakdown charts, and the full inventory table.

## Sample data

Synthetic only. The generator produces SKUs across categories such as beverages, dairy, packaged foods, personal care and home care, spread over locations in all four tiers, with 100 stock records. About one in five records is weighted toward a low-stock or near-expiry condition so the checks have cases to flag. No real sales, retailer or company data is used.

---

## Comparison with existing approaches (qualitative)

| | Manual / spreadsheet | Enterprise ERP | This system |
|---|---|---|---|
| Detection | Periodic review | Real-time after setup | On demand, automated |
| Cost and setup | Low | High licensing, long implementation | Open-source, free-tier hosting |
| Batch traceability | Weak | Strong | Movement log per stock record |
| Fit for one depot or store cluster | Yes | Over-sized | Yes |

This comparison is based on the literature and the architecture, not on live testing against an ERP.

## Limitations

- Validated on synthetic data only.
- Single-organisation scope; no supplier or retailer integration.
- Rule-based only; no demand forecasting.
- Thresholds are set by hand per SKU or category.
- The composite risk score is designed but not yet implemented in the prototype.
- Shelf-life-remaining checks (percentage of shelf life left, as retailers apply on receipt) are not yet implemented; the prototype uses days to expiry.

## Roadmap

1. Move the schema and detection logic to cloud-hosted MySQL behind a Spring Boot API.
2. Implement the composite risk score and connect it to the alert engine.
3. Add shelf-life-remaining and days-of-cover checks.
4. Add email/SMS alerts and role-based access control.
5. Extend to ML-based demand forecasting and multi-node visibility.

## References

1. R. K. Khokrale, "Using cloud-based ERP solutions for real-time supply chain visibility and decision-making," *IJRPETM*, vol. 7, 2024.
2. T. K. Agrawal et al., "Supply chain visibility: A Delphi study on managerial perspectives and priorities," *Int. J. Production Research*, vol. 62, 2024.
3. F. Balaha et al., "An analytical review of data integration for decision support in smart manufacturing," *Decision Analytics Journal*, vol. 17, 2025.
4. M. Sharma, "AI integration in ERP evaluation across trends and architectures," *JISEM*, vol. 10, 2025.
5. K. J. Harianto et al., "The effect of digital ERP implementation, supply chain integration and supply chain flexibility on business performance," *IJDNS*, vol. 8, 2024.
6. R. Uthayakumar and S. Priyan, "Pharmaceutical supply chain and inventory management strategies," *Operations Research for Health Care*, vol. 2, 2013.
7. R. Pall et al., "Predicting drug shortages using pharmacy data and machine learning," *Health Care Management Science*, vol. 26, 2023.
8. K. Yahya et al., "Hybrid machine learning forecasting for resilient and sustainable pharmaceutical supply chains," *Frontiers in Artificial Intelligence*, vol. 9, 2026.
