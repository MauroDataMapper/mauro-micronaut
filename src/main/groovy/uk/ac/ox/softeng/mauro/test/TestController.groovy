package uk.ac.ox.softeng.mauro.test

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import reactor.core.publisher.Mono

@Controller('/test')
class TestController {

    @Get('/{id}')
    Mono<Map> show(UUID id) {
        Mono.just([id: id])
    }

    @Get('/other')
    Mono<Map> show() {
        Mono.just([other: 'other'])
    }

    @Get('/optional{/id}')
    Mono<Map> optional(@Nullable UUID id) {
        Mono.just([optional: id])
    }

    @Get
    Mono<String> index() {
        Mono.just('index')
    }

}
