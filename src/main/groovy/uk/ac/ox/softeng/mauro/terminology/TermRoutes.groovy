//package uk.ac.ox.softeng.mauro.terminology
//
//import io.micronaut.context.ApplicationContext
//import io.micronaut.web.router.GroovyRouteBuilder
//import jakarta.inject.Inject
//import jakarta.inject.Singleton
//
//
//@Singleton
//class TermRoutes extends GroovyRouteBuilder {
//
//    TermRoutes(ApplicationContext beanContext) {
//        super(beanContext)
//    }
//
//    @Inject
//    def terms(TermController termController) {
//        GET(termController) {
//            GET('/tree{/id}', termController::tree)
//            GET('/{id}', termController::show)
//        }
//    }
//}
