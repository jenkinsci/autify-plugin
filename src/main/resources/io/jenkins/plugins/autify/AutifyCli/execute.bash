#!/bin/bash
while IFS= read -r line; do
  export PATH="$line:$PATH"
done < "./autify/path"

"$@"
