#!/bin/bash
# JVM Monitoring Script for TechMart

# Monitor JVM metrics
echo "=== JVM Metrics ==="
jstat -gc $(pgrep -f payara) 1000 10

# Monitor heap usage
echo "=== Heap Usage ==="
jmap -heap $(pgrep -f payara)

# Monitor thread usage
echo "=== Thread Usage ==="
jstack -l $(pgrep -f payara) | grep -E "state|Number of threads"

# Monitor GC logs
echo "=== Recent GC Activity ==="
tail -n 50 /opt/payara/logs/gc.log

# Monitor connection pool
echo "=== Connection Pool Status ==="
mysqladmin -u techmart -p status
