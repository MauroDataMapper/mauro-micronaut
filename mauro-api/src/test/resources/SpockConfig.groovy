import org.spockframework.runtime.model.parallel.ExecutionMode

runner {
    parallel {
        enabled false // enabling this requires enabling random server ports on tests
        defaultExecutionMode ExecutionMode.SAME_THREAD
    }
}