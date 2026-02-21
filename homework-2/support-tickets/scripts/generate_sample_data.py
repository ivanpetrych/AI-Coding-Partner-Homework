#!/usr/bin/env python3
import json
import random
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(SCRIPT_DIR, "../src/test/resources/data")
os.makedirs(DATA_DIR, exist_ok=True)

CATEGORIES = ["account_access", "technical_issue", "billing_question", "feature_request", "bug_report", "other"]
PRIORITIES = ["urgent", "high", "medium", "low"]
STATUSES = ["new", "in_progress", "waiting_customer", "resolved", "closed"]
AGENTS = ["Agent Smith", "Agent Jones", "Agent Brown"]

def generate_ticket(i):
    return {
        "customer_id": f"CUST{i:03d}",
        "customer_email": f"user{i}@example.com",
        "customer_name": f"User {i}",
        "subject": f"Sample Issue {i}",
        "description": f"This is a generated description for issue number {i}.",
        "category": random.choice(CATEGORIES),
        "priority": random.choice(PRIORITIES),
        "status": random.choice(STATUSES),
        "assigned_to": random.choice(AGENTS) if random.random() > 0.5 else None,
        "tags": ["generated", "sample"]
    }

# 1. CSV (50 tickets)
csv_lines = ["customer_id,customer_email,customer_name,subject,description,category,priority,status,assigned_to,tags"]
for i in range(1, 51):
    t = generate_ticket(i)
    # CSV handling simple strings
    tags = ";".join(t["tags"])
    assigned = t["assigned_to"] if t["assigned_to"] else ""
    line = f"{t['customer_id']},{t['customer_email']},{t['customer_name']},{t['subject']},{t['description']},{t['category']},{t['priority']},{t['status']},{assigned},{tags}"
    csv_lines.append(line)

with open(os.path.join(DATA_DIR, "sample_tickets.csv"), "w") as f:
    f.write("\n".join(csv_lines))
print(f"Generated {DATA_DIR}/sample_tickets.csv (50 tickets)")

# 2. JSON (20 tickets)
json_tickets = []
for i in range(51, 71):
    t = generate_ticket(i)
    # Convert snake_case to camelCase for Java/JSON compatibility if needed, 
    # but based on prompt requirements let's support snake_case and hope ImportService handles it or is updated.
    # However, existing code seems to prefer camelCase for JSON.
    # Let's generate camelCase JSON keys to be compatible with standard Jackson.
    camel_ticket = {
        "customerId": t["customer_id"],
        "customerEmail": t["customer_email"],
        "customerName": t["customer_name"],
        "subject": t["subject"],
        "description": t["description"],
        "category": t["category"],
        "priority": t["priority"],
        "status": t["status"],
        "assignedTo": t["assigned_to"],
        "tags": t["tags"]
    }
    json_tickets.append(camel_ticket)

with open(os.path.join(DATA_DIR, "sample_tickets.json"), "w") as f:
    json.dump(json_tickets, f, indent=2)
print(f"Generated {DATA_DIR}/sample_tickets.json (20 tickets)")

# 3. XML (30 tickets)
xml_lines = ["<TicketsXmlWrapper>", "  <tickets>"]
for i in range(71, 101):
    t = generate_ticket(i)
    # XML also usually maps to camelCase fields if using standard Jackson XmlMapper without annotations
    xml_lines.append("    <item>")
    xml_lines.append(f"      <customerId>{t['customer_id']}</customerId>")
    xml_lines.append(f"      <customerEmail>{t['customer_email']}</customerEmail>")
    xml_lines.append(f"      <customerName>{t['customer_name']}</customerName>")
    xml_lines.append(f"      <subject>{t['subject']}</subject>")
    xml_lines.append(f"      <description>{t['description']}</description>")
    xml_lines.append(f"      <category>{t['category']}</category>")
    xml_lines.append(f"      <priority>{t['priority']}</priority>")
    xml_lines.append(f"      <status>{t['status']}</status>")
    if t['assigned_to']:
        xml_lines.append(f"      <assignedTo>{t['assigned_to']}</assignedTo>")
    
    xml_lines.append("      <tags>")
    for tag in t['tags']:
        xml_lines.append(f"        <tag>{tag}</tag>")
    xml_lines.append("      </tags>")
    xml_lines.append("    </item>")

xml_lines.append("  </tickets>")
xml_lines.append("</TicketsXmlWrapper>")

with open(os.path.join(DATA_DIR, "sample_tickets.xml"), "w") as f:
    f.write("\n".join(xml_lines))
print(f"Generated {DATA_DIR}/sample_tickets.xml (30 tickets)")
