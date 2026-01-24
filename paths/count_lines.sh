#!/bin/bash

echo "=== Lines of Code Summary ==="
echo ""

# Count by file type
echo "By file type:"
for ext in kt java kts xml json; do
    count=$(find . -name "*.$ext" -type f 2>/dev/null | xargs wc -l 2>/dev/null | tail -1 | awk '{print $1}')
    files=$(find . -name "*.$ext" -type f 2>/dev/null | wc -l | tr -d ' ')
    if [ "$files" -gt 0 ]; then
        printf "  %-10s %6s lines (%s files)\n" ".$ext" "$count" "$files"
    fi
done

echo ""
echo "Total:"
find . -type f \( -name "*.kt" -o -name "*.java" -o -name "*.kts" \) | xargs wc -l 2>/dev/null | tail -1
