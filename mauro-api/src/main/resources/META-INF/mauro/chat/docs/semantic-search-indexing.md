# Semantic Search and Model Indexing

## Purpose

Semantic search lets Mauro find catalogue items by meaning rather than by exact words.
It complements keyword search; it does not replace it.
Keyword search remains the best tool for exact labels, quoted phrases, identifiers, and PostgreSQL full-text syntax.
Semantic search is for nearby concepts, related wording, and discovery when the user does not know the exact Mauro vocabulary.

The design in this module is intentionally conservative.
Semantic indexing can be expensive, especially when a catalogue contains large models.
The system therefore indexes only explicit declarations.
Enabling the semantic feature must not mean "index the whole catalogue".
It means "make semantic functionality available, then process the model indexes that have been deliberately declared".

The feature is currently implemented in the chat module, but it is shaped as a future plugin.
Core services expose contracts and no-op stubs; the operational implementation lives outside core ownership boundaries as much as possible.

## Top-Level Concepts

### Corpus

A corpus identifies a partition around the kind of material being indexed. The default public corpus is:

```text
catalogue-items
```

That corpus contains searchable Mauro catalogue content such as Data Models, Data Classes, Data Elements, Terminologies, Code Sets, and related metadata.

Corpus is a first-class concept: not all future semantic content belongs in the same search space.
For example, semantic skill suggestion or prompt retrieval would be a different corpus from catalogue item search.
Keeping corpora explicit gives us:

- separate visibility rules for public API corpora and internal corpora;
- a clean way to scope searches;
- a clean way to delete or maintain one body of semantic data without touching another;
- future freedom to add internal semantic features without exposing their chunks through public API routes.

An API-created corpus is discoverable through the public corpus API.
Internal corpora can remain hidden and unmanaged by public routes.

### Embedding Profile

An embedding profile describes how text is embedded into vectors. It includes:

- provider, for example `ollama`;
- embedding model, for example `nomic-embed-text`;
- vector dimension;
- distance metric, normally `cosine`;
- enabled/disabled state;
- a human description.

Embedding profiles are global capabilities. An embedding profile being enabled means it may be used by searches and indexes that declare it.
It does not mean every model in the catalogue should be indexed with that embedding profile.

Multiple embedding profiles can be attached to the same model and corpus.
This is useful when one embedding model is general-purpose and another is domain-specific, such as a medically trained embedding model for forms about clinical workflows.

### Model Index

A model index is the declaration that makes semantic indexing concrete:

```text
model_id x corpus_name x embedding_profile
```

This tuple is the central unit of semantic indexing. It says:

"For this Mauro model scope, in this corpus, using this embedding profile, maintain a semantic index."

No declaration means there is no indexing.

The `model_id` can refer to a DataModel, Terminology, CodeSet, Folder, or VersionedFolder.
When it refers to a Folder or VersionedFolder, it represents the descendant DataModels, Terminologies, and CodeSets that exist within that folder scope *at the point* of that indexing/searching.
This matches scoped search semantics elsewhere in Mauro.

### Chunk

A chunk is the text unit embedded for semantic retrieval.
Chunks are generated from catalogue content: labels, descriptions, summaries, identifiers, and other text that is likely to carry meaning.

Chunking deliberately includes small label-derived chunks for database-style identifiers. For example:

```text
Date_Created
```

can produce meaningful label fragments such as:

```text
Date Created
```

This helps semantic search find terms near a conceptual cluster without turning semantic search into exact lexical search.

Duplicate chunks are avoided where the same source would otherwise produce identical text for multiple chunk kinds.
That keeps storage, embedding work, result evidence, and network payloads smaller.

### Embedding

An embedding is the vector representation of a chunk for one embedding profile.
The same chunk can have multiple embeddings, for example, one for a general profile and another for a domain-specific profile.

Adding a new embedding profile to an already-indexed model does not require new chunks.
It requires new embeddings for the existing chunks for that profile.

### Job

Indexing work runs as jobs. Jobs exist because indexing can be slow, can be interrupted by restarts, and must be observable.

Jobs expose:

- status, such as `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `INTERRUPTED`, and `TO_RESTART`;
- model/corpus/embedding profile scope;
- whether embeddings are being rebuilt;
- progress/result JSON;
- event stream as newline-delimited JSON;
- timestamps.

Jobs are recovered after the application restarts.
Recoverable jobs are resubmitted, so manual external intervention is not required.

### Global Indexing Controls

There are two separate but related controls:

- indexing enabled;
- auto reconcile enabled.

Indexing enabled says whether indexing is switched on at all.
Auto reconcile says whether declared model indexes that have changes should be automatically reprocessed after the detection of catalogue changes.

The default is the safe one being the least possible work:

```text
indexing: disabled
auto reconcile: disabled
```

An explicit start request can still run a job while global indexing is disabled if the request says `runWhenIndexingDisabled: true`.
This is useful for controlled manual testing.

## Why It Is Organised This Way

### Explicit Declarations Prevent Surprise Work

The catalogue can contain enormous models.
A single broad "turn on semantic indexing" switch would be unsafe because it might start hours of embedding work across data that nobody intended to index.

The model-index declaration makes the operational decision explicit:

```text
Index this model scope, in this corpus, using this embedding profile.
```

That is auditable, inspectable, and reversible.

### Embedding Profiles and Corpora Keep Concerns Separate

Embedding profiles answer "how is text represented as vectors?"

Corpora answer "what body of text is this?"

Model indexes answer "which model scope participates?"

Keeping these separate avoids coupling future semantic use cases to catalogue search.
It also lets a team use a domain-specific embedding profile only where it helps, instead of applying it to every catalogue item.

### Jobs Make Long Work Safe

Indexing is not a request/response operation in practice.
Jobs give the system a durable state machine, recovery path, cancellation, and progress reporting.
They also make the user workflow scriptable with `curl`, automation, or via a bespoke UI.

### Search Is Model-Aware

Semantic availability is not global.
It depends on whether semantic indexes exist for the requested model and corpus scope.

When a search is scoped by model, semantic search uses the embedding profiles and corpora declared for that model scope.
When a search is unscoped, it searches available API-visible corpora.
Corpus and model constraints combine to narrow the search area.

### Hybrid Search Uses Semantic When Available

Hybrid search is to become the ordinary search surface.
It combines keyword retrieval and semantic retrieval when semantic search is available.
If semantic search is unavailable, it behaves exactly like the default keyword search.

Hybrid search uses bounded retrieval and short-lived candidate caching so normal searches and paging remain responsive.
Deep semantic recall is still available through `deepSearch: true`, which does not bound the retrieval and takes much longer to run.

## Feature Overview

The implementation provides:

- API-visible and internal corpora;
- embedding profile creation, enable, disable, delete;
- embedding model pull/probe support;
- dimension inference for embedding profiles where the provider can supply it;
- exact model index declarations for `model_id x corpus_name x profile_name`;
- model index labels for readable administration output;
- scoped action routes that can apply to all profiles/corpora;
- global indexing and auto-reconcile controls;
- manual job start with `runWhenIndexingDisabled`;
- job list, status, cancel, resume, and event streaming;
- restart recovery for queued/running/interrupted jobs;
- stale tracking for declared model indexes;
- auto reconcile that processes only stale declared model indexes;
- corpus-aware and model-aware semantic search;
- profile-aware semantic search for diagnostics and specialist searches;
- hybrid search that falls back to keyword search when semantic search is unavailable;
- normal fast semantic candidate windows and `deepSearch` for broader recall;
- cached hybrid candidates for faster paging while preserving per-request access filtering.

## Operational Story

This section describes a realistic workflow from first inspection to useful search.
It assumes an administrator is using `curl` and an `apiKey` header.

```shell
API_KEY='...'
BASE='http://localhost:8080'
```

### 1. Understand the Current Semantic State

Start by checking whether indexing is enabled and whether automatic reconcile is enabled:

```shell
curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/indexing" | jq
```

A safe default looks like indexing disabled and auto reconcile disabled.

List corpora:

```shell
curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/corpora" | jq
```

The default public corpus should normally be `catalogue-items`.

List embedding profiles:

```shell
curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/profiles" | jq
```

Embedding profiles tell you which embedding models are available, enabled, and ready to use.

List declared model indexes:

```shell
curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/modelIndexes" | jq
```

This endpoint is intentionally lightweight.
It is for administration and should show keys, labels, corpus, profile, enabled state, and status.
Detailed counts live under stats.

For a model-specific detailed view:

```shell
MODEL_ID='...'

curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/modelIndexes/$MODEL_ID/stats" | jq
```

### 2. Add or Prepare an Embedding Profile

If the embedding provider manages the embedding model you would like to use, it can be pulled into the embedding provider and will become an embedding profile:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  -H 'Content-Type: application/json' \
  "$BASE/api/semanticSearchIndex/embeddingModels:pull" \
  -d '{"provider":"ollama","model":"nomic-embed-text"}' | jq
```

Some Ollama models may be installed from Hugging Face or another local mechanism rather than pulled from the Ollama catalogue.
In that case, create the embedding profile directly.

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  -H 'Content-Type: application/json' \
  "$BASE/api/semanticSearchIndex/profiles" \
  -d '{
    "name": "ollama-nomic-embed-text",
    "provider": "ollama",
    "embeddingModel": "nomic-embed-text",
    "distanceMetric": "cosine",
    "description": "General purpose Ollama embeddings"
  }' | jq
```

If `dimension` is omitted, the implementation attempts to infer it by asking the embedding provider.
`cosine` is the default distance metric when not specified.

Enable the profile:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/profiles/ollama-nomic-embed-text:enable" | jq
```

Profiles can be disabled without deleting embeddings:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/profiles/ollama-nomic-embed-text:disable" | jq
```

That is useful for comparing search behaviour between embedding profiles.

### 3. Declare a Model Index

Choose a model scope and a profile.
The model scope can be a folder if the intended scope is all descendant DataModels, Terminologies, and CodeSets in that folder.

```shell
MODEL_ID='16a00d09-b8e5-47f2-95a4-752e47852510'
PROFILE='ollama-nomic-embed-text'
CORPUS='catalogue-items'
```

Create the declaration:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  -H 'Content-Type: application/json' \
  "$BASE/api/semanticSearchIndex/modelIndexes" \
  -d "{
    \"modelId\": \"$MODEL_ID\",
    \"corpusName\": \"$CORPUS\",
    \"profileName\": \"$PROFILE\",
    \"enabled\": true,
    \"label\": \"Forms general embeddings\"
  }" | jq
```

This creates or updates one exact tuple:

```text
modelId x corpusName x profileName
```

Creation is deliberately exact: the caller must supply the corpus and profile so the declaration is not ambiguous.

### 4. Start Indexing

If global indexing is disabled, a normal start request will be refused with a helpful message:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  -H 'Content-Type: application/json' \
  "$BASE/api/semanticSearchIndex/corpora/$CORPUS/modelIndexes/$MODEL_ID/profiles/$PROFILE:start" \
  -d '{}' | jq
```

To run the explicit job anyway:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  -H 'Content-Type: application/json' \
  "$BASE/api/semanticSearchIndex/corpora/$CORPUS/modelIndexes/$MODEL_ID/profiles/$PROFILE:start" \
  -d '{"runWhenIndexingDisabled": true}' | jq
```

`rebuildEmbeddings` defaults to `false`.
That means existing valid embeddings are reused.
Set it to `true` only when the embeddings should be rebuilt.

The action URL shape is intentional:

```text
/api/semanticSearchIndex/corpora/{corpusName}/modelIndexes/{modelId}/profiles/{profileName}:start
```

If `corpora/{corpusName}` is omitted, the action applies across matching corpora.
If `/profiles/{profileName}` is omitted, it applies across matching profiles.
Omission means "all in that dimension"; creation does not allow that ambiguity.

### 5. Watch Jobs

List active jobs:

```shell
curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/jobs" | jq
```

Include history:

```shell
curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/jobs?includeHistory=true" | jq
```

Inspect one job:

```shell
JOB_ID='...'

curl -s \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/jobs/$JOB_ID" | jq
```

Follow JSON-lines events:

```shell
curl -N \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/jobs/$JOB_ID/events?follow=true"
```

The event stream shows state transitions and progress.
Long-running indexing jobs also recover across application restart.
A running job becomes interrupted on shutdown and is resubmitted when the application starts again.

Cancel a job:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/jobs/$JOB_ID:cancel" | jq
```

Resume a recoverable job:

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/jobs/$JOB_ID:resume" | jq
```

### 6. Turn On Automatic Maintenance

Once the desired model indexes exist, automatic maintenance can be enabled.
Automatic maintenance can be enabled before now, but now is a logical time to switch this on.

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/indexing/autoReconcile:enable" | jq
```

Enable indexing.
Indexing can also be enabled before now.

```shell
curl -s -X POST \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/indexing:enable" | jq
```

When catalogue content changes and search domains refresh, auto reconcile checks the declared enabled model indexes.
Stale declarations are processed. Undeclared models are ignored.

This is the key safety property:

```text
No model index declaration, no semantic indexing work.
```

### 7. Search Semantically

Semantic search is available through API and MCP surfaces.
A model-scoped semantic search searches the corpora and embedding profiles declared for that model scope:

```json
{
  "query": "diabetes",
  "modelId": "16a00d09-b8e5-47f2-95a4-752e47852510"
}
```

An embedding profile-scoped diagnostic search can compare embedding profiles:

```json
{
  "query": "diabetes",
  "embeddingProfiles": ["ollama-nomic-embed-text"]
}
```

and:

```json
{
  "query": "diabetes",
  "embeddingProfiles": ["ollama-embeddinggemma"]
}
```

A corpus can also be supplied:

```json
{
  "query": "diabetes",
  "corpus": "catalogue-items",
  "modelId": "16a00d09-b8e5-47f2-95a4-752e47852510"
}
```

Typical semantic search uses a 'bounded candidate window' for speed.
`deepSearch: true` asks for broader recall and will be slower:

```json
{
  "query": "diabetes",
  "modelId": "16a00d09-b8e5-47f2-95a4-752e47852510",
  "deepSearch": true
}
```

### 8. Use Hybrid Search for Ordinary Search

The main search path is a hybrid search.
When semantic search is available, it combines keyword and semantic candidates.
When semantic search is unavailable, it behaves as keyword search.

Hybrid search is useful because:

- keyword search catches exact labels and identifiers;
- semantic search catches related concepts;
- access filtering is applied before results are returned;
- paging reuses cached pre-access candidates, so subsequent pages avoid repeating keyword and vector retrieval.

New concept: *counts may be approximate* when the system deliberately stops once it has enough readable results for a page.
Responses expose whether the counts are exact or not.

## Performance and Tuning

Typical semantic search is tuned for responsiveness.
It uses a smaller candidate window and corresponding HNSW search breadth.
`deepSearch` keeps the high-recall path available for slower, broader investigations.

Hybrid search uses:

- bounded keyword retrieval;
- bounded semantic retrieval;
- merged candidate ranking;
- per-request access filtering;
- short-lived pre-access candidate caching for paging.

The cache is deliberately applied before access filtering.
That means it can be reused without sharing user-visible result pages across users.
Access checks still run per request.

### Default Embedding Profile

The default embedding profile is a convenience for API routes and internal calls where an embedding profile is optional.
It should point at a real embedding profile name, for example:

```text
ollama-nomic-embed-text
```

It does not create that embedding profile, enable it, create a model index, or start indexing.
It only answers the question "which embedding profile should be assumed when the caller deliberately omits one?"

For administration and repeatable automation, prefer exact model index declarations that include `modelId`, `corpusName`, and `profileName`.

### Embedding Batch Size and Adaptive Backoff

Embedding jobs process chunks in batches.
The configured batch size is the normal number of chunks sent to the embedding provider and then upserted into the database in one unit.

Larger batches can improve throughput because they reduce per-request overhead, but they also increase:

- memory pressure;
- provider timeout risk;
- the cost of retrying a failed batch;
- latency before progress is visible.

If a batch fails, the indexer retries it adaptively as smaller sub-batches.
The retry size is reduced until it reaches the configured minimum adaptive batch size.
If a batch still fails at that minimum size, the job fails rather than silently skipping chunks.

This gives large indexing jobs a useful middle ground:

- normal operation can use efficient batches;
- transient provider limits can be worked around automatically;
- persistent bad data or provider failure is still visible as a failed job.

The per-job start request can override `batchSize`, so large administrative runs and small test runs do not need the same operational setting.

### Duplicate-Content Embedding Reuse

`reuse-duplicate-content-before-embedding` controls an optional database pass that runs before the indexer asks the embedding provider for new vectors.
It is off by default.

The pass happens after:

1. chunks have been reconciled;
2. stale embeddings for the embedding profile have been deleted;
3. before the job counts chunks still needing embeddings.

It looks for chunks in the current model/corpus scope that do not currently have a valid embedding for the selected embedding profile.
For each such chunk, it tries to find an existing embedding for the same embedding profile with the same `content_hash`.
If it finds one, it copies that stored vector instead of sending the duplicate text to the embedding provider again.

This is useful when a catalogue contains repeated text, for example:

- repeated labels;
- repeated metadata values;
- repeated inherited/context chunks;
- repeated form or template wording.

It can reduce provider calls, cost, and indexing time when duplicate content is common.
It is less helpful when most chunks are unique.

The tradeoff is that it adds a database matching pass over the scoped chunks before embedding starts.
That pass is index-backed by embedding profile and content hash, but it still has to inspect the candidate chunk set and insert or update reused embedding rows.
On a small model it should usually be quick.
On a large folder or broad corpus it can take noticeable time, especially if many reused rows are inserted.
The job logs report whether the pass is skipped or enabled and how many duplicate-content embeddings were reused.

The pass is skipped when `rebuildEmbeddings` is true because a forced rebuild means "create embeddings again" rather than reusing previous vectors.
Keep the setting off unless testing shows duplicate reuse materially reduces embedding work for the catalogue.

### Deferred Vector Index Maintenance

Embeddings are stored in PostgreSQL with pgvector.
The HNSW vector index makes search fast, but maintaining it while inserting a large number of embeddings can make bulk indexing slower.

For large embedding loads, the indexer can defer HNSW maintenance:

1. drop or avoid the vector index before the bulk load;
2. insert or update embeddings;
3. rebuild the vector index after the embedding load completes.

This is an indexing throughput optimisation, not a modelling concept.
While the HNSW index is absent, *semantic search for that embedding profile may be unavailable or much slower*, depending on the database query path and data volume.
The intended operational model is that a large job completes and rebuilds the vector index before the embedding profile is relied on for interactive search.

If an embedding load fails part-way through while vector index maintenance is deferred, the default is conservative: leave the HNSW index absent and make the failed job visible.
Rerunning the indexing job resumes from stored chunks/embeddings and rebuilds the vector index following a successful completion.
`rebuild-vector-index-after-partial-load` can force a rebuild after partial failure, but that may expose a partially refreshed vector set as searchable.

### Candidate Windows and HNSW Breadth

Search performance is mostly controlled by how many vector candidates are requested and how much work pgvector does to find them.
The normal candidate window is deliberately bounded for interactive use.
`deepSearch` increases the window for slower, higher-recall investigations.

The HNSW `ef_search` settings control nearest-neighbour search breadth.
Higher values may improve recall but increase latency.
In practice, candidate window size and `ef_search` should be tuned together from telemetry rather than guessed.

Some useful telemetry can appear in logs:

```text
Semantic search timing ... candidateCounts=... vectorTimingsMs=... queryEmbeddingTimingsMs=... timingsMs=...
Hybrid search timing ... cacheHit=... keywordLimit=... timingsMs=...
```

This telemetry is part of the operating model.
During development, it helped identify that excessive ordinary `topN`/HNSW breadth was the cause of slow broad semantic searches, and it could remain available while the feature matures.

## Deleting and Cleaning Up

Delete a model index declaration:

```shell
curl -s -X DELETE \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/corpora/$CORPUS/modelIndexes/$MODEL_ID/profiles/$PROFILE" | jq
```

Delete the declaration and embeddings produced for that model index:

```shell
curl -s -X DELETE \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/corpora/$CORPUS/modelIndexes/$MODEL_ID/profiles/$PROFILE?deleteEmbeddings=true" | jq
```

Delete embeddings while leaving the declaration:

```shell
curl -s -X DELETE \
  -H "apiKey: $API_KEY" \
  "$BASE/api/semanticSearchIndex/corpora/$CORPUS/modelIndexes/$MODEL_ID/profiles/$PROFILE/embeddings" | jq
```

Corpus configuration intentionally controls corpus chunk deletion.
'Internal' corpora should not be exposed to public destructive API operations.

## Glossary

### API-Visible Corpus

A corpus that can be listed, selected, and managed through the public semantic indexing API.
The default API-visible corpus is `catalogue-items`.

### Auto Reconcile

The background maintenance mode that reacts to refreshed search-domain data and checks declared model indexes for staleness.
When enabled, it processes stale, enabled model index declarations only.
It does not index undeclared models.

### Candidate Window

The number of nearest vector candidates requested before reranking and access filtering.
Normal searches use a smaller candidate window for responsiveness.
`deepSearch` uses a broader candidate window for recall.

### Chunk

A unit of text extracted from catalogue content and stored for semantic embedding.
Chunks may come from labels, descriptions, summaries, identifiers, metadata, and other meaningful text.

### Chunk Group

A grouping label for chunks and embeddings, such as `catalogue` or `context`.
Chunk groups allow different retrieval paths and HNSW settings without redefining the corpus.

### Corpus

A named body of semantic content.
`catalogue-items` is the default corpus for Mauro catalogue search.
Future internal features may use separate corpora for non-catalogue content such as skills or prompts.

### Deep Search

A search mode that favours broader recall over speed.
It increases the semantic candidate window and HNSW search breadth.
Use it for investigation, not as the default for ordinary UI or tool searches.

### Default Embedding Profile

The embedding profile name assumed by routes or calls that intentionally omit an embedding profile.
It is a convenience default only; it does not create declarations or start indexing.

### Deferred Vector Index Maintenance

An optional indexing optimisation where HNSW vector index maintenance is paused for a large embedding load and rebuilt after the load completes.
It can make large jobs faster, but search should not rely on that embedding profile until the vector index has been rebuilt.

### Duplicate-Content Embedding Reuse

An optional pre-embedding database pass that copies an existing embedding for the same embedding profile and same content hash onto duplicate chunks.
It can reduce provider calls when repeated text is common, but it adds database work before embedding starts.

### Embedding

The vector representation of one chunk for one embedding profile.
The same chunk can have multiple embeddings if multiple embedding profiles are attached to the same model index scope.

### Embedding Batch Backoff

The adaptive retry behaviour used when an embedding batch fails.
The indexer retries smaller sub-batches until it reaches the configured minimum adaptive batch size.

### Embedding Batch Size

The number of chunks the indexer sends to the embedding provider in one request during an indexing job.
It can be configured globally and overridden for an individual job start request.

### Embedding Model

The provider-specific model that turns text into a vector.
For example, an Ollama model such as `nomic-embed-text`.

### Embedding Profile

The Mauro configuration record describing how to create and search embeddings.
It names the provider, embedding model, vector dimension, distance metric, enabled state, and description.
Use this term rather than just "profile" to avoid confusion with other Mauro profile concepts.

### HNSW

The approximate nearest-neighbour index used by pgvector for fast vector search.
Its search breadth is controlled by `hnsw.ef_search`; larger values can improve recall but increase latency.

### Hybrid Search

The ordinary combined search path.
When semantic search is available, it merges keyword and semantic candidates.
When semantic search is unavailable, it behaves as keyword search.

### Indexing Enabled

The global control that allows ordinary semantic indexing jobs to run.
Explicit manual starts can still opt in with `runWhenIndexingDisabled`.

### Internal Corpus

A corpus that exists for system/plugin use but is not discoverable or manageable through public API routes.
This protects future internal semantic features from accidental public destructive operations.

### Job

A durable indexing task with status, timestamps, progress/result payloads, and event stream.
Jobs make long-running indexing observable, cancellable, and recoverable after application restart.

### Keyword Search

The PostgreSQL full-text search path.
It is best for exact words, labels, identifiers, quoted phrases, and syntax such as `OR` and exclusions.

### Model Index

The explicit declaration that a Mauro model scope should be semantically indexed for one corpus and one embedding profile.
Its logical key is:

```text
model_id x corpus_name x embedding_profile
```

### Model Scope

The set of catalogue models represented by a `modelId`.
If the id is a Folder or VersionedFolder, the scope includes descendant DataModels, Terminologies, and CodeSets.

### Rebuild Embeddings

A job option that forces embeddings to be recreated rather than reusing valid existing embeddings.
It should default to false for normal operation.

### Run When Indexing Disabled

A manual job-start option that allows an explicit indexing job to run even when global indexing is disabled.
This is useful for controlled tests or one-off administrative work.

### Semantic Search

Vector-based search over embedded chunks.
It finds results by similarity of meaning rather than exact lexical matching.

### Stale Model Index

A model index declaration whose indexed chunks or embeddings may no longer match the catalogue content.
Auto reconcile processes stale enabled declarations.

## Example YAML Configuration

The safest application configuration is deliberately small.
Global indexing and auto-reconcile are runtime controls managed through the API and stored in `semantic.semantic_indexing_control`; they are not normally enabled by YAML.
This means a deployment can start without beginning semantic indexing work, then an administrator can enable indexing only after embedding profiles and model indexes have been declared.

```yaml
chat:
  semantic:
    # Used by API routes that allow the embedding profile to be omitted.
    # It does not by itself create a model index or start indexing.
    default-embedding-profile: ollama-nomic-embed-text

    embeddings:
      # Number of chunks sent to the embedding provider per batch.
      # The API start request can override this for an individual job.
      # Larger values can improve throughput but should be tested with the provider.
      # e.g. 1536
      batch-size: 512

      # Failed batches are split and retried down to this size before the job fails.
      adaptive-min-batch-size: 128

      # Optional pre-embedding database pass that copies embeddings for identical
      # content hashes. Can save provider calls when duplicate text is common,
      # but adds database work before embedding starts.
      reuse-duplicate-content-before-embedding: false

      # Large loads can defer HNSW index maintenance while embeddings are inserted.
      defer-vector-index-threshold: 10000
      defer-vector-index-with-existing-embeddings: false
      rebuild-vector-index-after-partial-load: false

      # Lower values give faster feedback in job events/logs at the cost of more noise.
      progress-log-interval-seconds: 30

      ollama:
        timeout-seconds: 60
        pull-timeout-seconds: 600

    indexing:
      # Keep this low unless the embedding provider and database can handle parallel jobs.
      worker-threads: 1

    search:
      # Normal search is intentionally bounded for interactive use.
      minimum-candidate-window: 100
      maximum-candidate-window: 200

      # Deep search is opt-in and favours recall over latency.
      deep-candidate-window: 1600

      # Fetch extra candidates so access filtering can still fill the requested page.
      access-filter-fetch-multiplier: 20

      # Context search is optional and normally off for the catalogue path.
      include-context: false

      # Cache query embeddings by provider/model/query text.
      query-embedding-cache-size: 1000

      # pgvector HNSW search breadth. Higher values may improve recall but cost latency.
      hnsw-ef-search: 10
      catalogue-hnsw-ef-search: 10
      context-hnsw-ef-search: 40

    hybrid:
      # Reciprocal-rank-fusion weights for keyword and semantic candidates.
      keyword-weight: 1.0
      semantic-weight: 1.0
      rank-constant: 60

      # How many semantic candidates are requested by hybrid search before merging.
      rank-window: 100

      # When semantic search is available, keyword search is bounded for responsiveness.
      keyword-fetch-multiplier: 10

      # Cache merged pre-access-filter hybrid candidates to make paging cheaper.
      result-cache-size: 128
      result-cache-ttl-ms: 300000
```

For a least-work startup, omit this section entirely or keep the values close to the defaults above.
Then use the API to create or enable embedding profiles, create explicit model indexes, enable auto-reconcile if desired, and finally enable indexing.
