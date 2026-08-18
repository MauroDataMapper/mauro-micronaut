# Shred Function Refactoring: Visitor Pattern

## Overview

The `shred` function in `ContentHandler` has been refactored to use the **visitor pattern** with `GenericDomainTraversalVisitor` from the `mauro-domain` module. This eliminates multiple overloaded methods and provides a cleaner, more maintainable implementation.

## Previous Implementation

The original implementation used method overloading to handle different domain types:

```groovy
void shred(Folder folder, Integer depth = 0) { ... }
void shred(ClassificationScheme classificationScheme) { ... }
void shred(Classifier classifier) { ... }
void shred(Terminology terminology) { ... }
// ... etc for ~10+ types
```

Each method:
1. Added the item to the appropriate collection
2. Called `shredFacets()` to process metadata, annotations, etc.
3. Manually traversed children by calling `shred()` recursively

**Issues:**
- Difficult to maintain consistency across all types
- Easy to forget a type or its children
- Depth tracking for hierarchical structures was error-prone
- Hard to extend for new domain types

## New Implementation

### ShredVisitor Class

New file: `ShredVisitor.groovy`

The `ShredVisitor` extends `GenericDomainTraversalVisitor` and registers enter/leave handlers for each domain type:

```groovy
ShredVisitor visitor = new ShredVisitor()
visitor.shred(rootFolder)

// Collections are now available
Map<Integer, Set<Folder>> folders = visitor.folders
Set<DataModel> dataModels = visitor.dataModels
// etc.
```

**Key Features:**

1. **Type-Safe Handlers**: Each domain type has an `onEnter()` handler that collects the item:
   ```groovy
   onEnter(Folder) { Folder folder ->
       int currentDepth = folderDepthStack.isEmpty() ? 0 : folderDepthStack.peek() + 1
       addFolderAtDepth(folder, currentDepth)
       folderDepthStack.push(currentDepth)
   }
   ```

2. **Stack-Based Depth Tracking**: Uses separate stacks for folders and data classes to properly track nesting:
   - Folders use `folderDepthStack`
   - Data Classes use `dataClassDepthStack`
   - This prevents depth confusion when traversing multiple hierarchies

3. **Automatic Traversal**: The visitor pattern automatically handles:
   - Preventing revisits (via `shouldVisit()`)
   - Dispatch to correct handler based on type
   - Breadth-first traversal of the entire domain tree

### ContentHandler Changes

The `ContentHandler.shred()` method now:

```groovy
void shred(Folder folder) {
    ShredVisitor visitor = new ShredVisitor()
    visitor.shred(folder)
    
    // Copy collected items from visitor
    this.folders.putAll(visitor.folders)
    this.dataModels.addAll(visitor.dataModels)
    // ... etc
    
    // Process facets separately
    shredAllFacets()
}
```

The old `shredFacets()` method has been replaced with `shredAllFacets()`, which:
- Processes all collected items
- Extracts metadata, annotations, edits, rules, semantic links, etc.
- Tracks annotation depth properly now that facets are processed after full traversal

## Benefits

1. **Cleaner Code**: Single visitor class instead of 10+ overloaded methods
2. **Better Maintenance**: Adding a new domain type just requires registering a handler
3. **Correct Depth Tracking**: Stack-based approach handles nested hierarchies correctly
4. **Type Safety**: Each handler is type-checked at compile time
5. **Extensibility**: Easy to add new handlers for additional types or processing logic
6. **Consistency**: All domain types are processed uniformly by the framework

## How It Works

1. **Initialization**: `ShredVisitor` registers handlers for all domain types in `setupHandlers()`
2. **Traversal**: `visitor.shred(folder)` starts depth-first traversal from root folder
3. **Dispatch**: The visitor infrastructure calls `item.accept(this)` which dispatches to appropriate handler
4. **Collection**: Each handler adds the item to the appropriate collection with depth tracking
5. **Facet Processing**: After traversal completes, `shredAllFacets()` processes all facets
6. **Data Transfer**: Items are copied from visitor to ContentHandler for backwards compatibility

## Usage Example

```groovy
@Inject ContentHandler contentHandler

void someMethod(Folder rootFolder) {
    contentHandler.shred(rootFolder)
    
    // Now all items are organized in collections
    Map<Integer, Set<Folder>> folders = contentHandler.folders
    Set<DataModel> dataModels = contentHandler.dataModels
    
    // Save to database
    contentHandler.saveWithContent()
}
```

## Testing

Existing tests should pass without modification since the public API of `ContentHandler.shred()` remains the same. The collections populated by the new visitor should contain identical data to the original implementation.

## Future Enhancements

1. **Streaming Facet Processing**: Process facets during traversal instead of after, reducing memory usage for large datasets
2. **Parallel Processing**: Use the visitor registry to parallelize processing of independent items
3. **Custom Visitors**: Users can create their own visitors for different use cases (validation, transformation, etc.)

