#!/usr/bin/env python3
"""Extract JaCoCo and Surefire test metrics and write to results/coverage.json"""

import json
import csv
import xml.etree.ElementTree as ET
import glob
import os
from pathlib import Path

def read_jacoco_metrics():
    """Read coverage metrics from jacoco.csv"""
    metrics = {
        'INSTRUCTION_MISSED': 0,
        'INSTRUCTION_COVERED': 0,
        'LINE_MISSED': 0,
        'LINE_COVERED': 0,
        'BRANCH_MISSED': 0,
        'BRANCH_COVERED': 0,
    }

    jacoco_file = 'target/site/jacoco/jacoco.csv'
    if not os.path.exists(jacoco_file):
        return metrics

    with open(jacoco_file) as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Skip excluded classes
            if row.get('PACKAGE', '').startswith('benchmark/') or row.get('CLASS') == 'AgentEvalApplication':
                continue
            for key in metrics:
                if key in row:
                    metrics[key] += int(row[key])

    return metrics

def read_surefire_metrics():
    """Read test metrics from surefire-reports"""
    tests = 0
    failures = 0
    errors = 0
    skipped = 0

    for report_file in glob.glob('target/surefire-reports/*.xml'):
        try:
            tree = ET.parse(report_file)
            root = tree.getroot()
            for testcase in root.findall('.//testcase'):
                tests += 1
                if testcase.find('failure') is not None:
                    failures += 1
                if testcase.find('error') is not None:
                    errors += 1
                if testcase.find('skipped') is not None:
                    skipped += 1
        except Exception:
            pass

    return tests, failures, errors, skipped

def calculate_percentages(metrics):
    """Calculate coverage percentages"""
    line_total = metrics['LINE_MISSED'] + metrics['LINE_COVERED']
    line_pct = (metrics['LINE_COVERED'] / line_total * 100) if line_total > 0 else 0

    branch_total = metrics['BRANCH_MISSED'] + metrics['BRANCH_COVERED']
    branch_pct = (metrics['BRANCH_COVERED'] / branch_total * 100) if branch_total > 0 else 0

    instruction_total = metrics['INSTRUCTION_MISSED'] + metrics['INSTRUCTION_COVERED']
    instruction_pct = (metrics['INSTRUCTION_COVERED'] / instruction_total * 100) if instruction_total > 0 else 0

    return {
        'line_coverage_pct': round(line_pct, 1),
        'branch_coverage_pct': round(branch_pct, 1),
        'instruction_coverage_pct': round(instruction_pct, 1),
        'lines_covered': metrics['LINE_COVERED'],
        'lines_total': line_total,
    }

def main():
    jacoco_metrics = read_jacoco_metrics()
    tests, failures, errors, skipped = read_surefire_metrics()
    coverage_pcts = calculate_percentages(jacoco_metrics)

    result = {
        'tests': tests,
        'failures': failures,
        'errors': errors,
        'skipped': skipped,
        'line_coverage_pct': coverage_pcts['line_coverage_pct'],
        'branch_coverage_pct': coverage_pcts['branch_coverage_pct'],
        'instruction_coverage_pct': coverage_pcts['instruction_coverage_pct'],
        'lines_covered': coverage_pcts['lines_covered'],
        'lines_total': coverage_pcts['lines_total'],
        'jacoco_excludes': ['benchmark/**', 'AgentEvalApplication'],
    }

    # Create results directory if needed
    Path('results').mkdir(exist_ok=True)

    # Write output
    with open('results/coverage.json', 'w') as f:
        json.dump(result, f, indent=2)

    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()
