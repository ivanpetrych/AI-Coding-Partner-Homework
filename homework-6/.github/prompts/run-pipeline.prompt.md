---
description: "Run the multi-agent banking pipeline end-to-end. Use when: running pipeline, executing transactions, processing sample data, testing full pipeline."
---
Run the multi-agent banking pipeline end-to-end.

## Steps

1. **Check prerequisites**: Verify that `sample-transactions.json` exists in the `homework-6/` directory. If not found, report an error and stop.

2. **Clear shared/ directories**: Remove all files from `shared/input/`, `shared/processing/`, `shared/output/`, and `shared/results/` to ensure a clean run.

3. **Run the pipeline**: Execute the integrator from the `homework-6/` directory:
   ```bash
   cd homework-6 && python3 integrator.py
   ```

4. **Show results summary**: Read `shared/results/pipeline_summary.json` and display:
   - Total transactions processed
   - Validated / Rejected / Settled / Flagged counts
   - Risk breakdown (LOW / MEDIUM / HIGH)

5. **Report rejected transactions**: For each `.json` file in `shared/results/` where `final_status` is `"rejected"`, show:
   - Transaction ID
   - Amount and currency
   - Rejection reason

6. **Report flagged transactions**: For each result with `final_status` = `"flagged"`, show the transaction ID, amount, and risk factors.
