npm-groovy-lint --output json **/*.groovy **/*.gradle \
  | jq -r '
      . as $root
      | [
          $root.files
          | to_entries[]
          | .key as $file
          | .value.errors[]
          | {rule: .rule, file: $file}
        ]
      | group_by(.rule)
      | map({
          rule: .[0].rule,
          errors: length,
          files: ([.[].file] | unique | length)
        })
      | sort_by(-.errors)
      | .[]
      | "\(.errors)\t\(.files)\t\(.rule)"
    ' \
  | column -t
