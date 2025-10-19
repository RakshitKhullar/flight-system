package com.travel_search.travel_search_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan(basePackages = ["com.travel_search.travel_search_service.repository.entity"])
@EnableJpaRepositories(basePackages = ["com.travel_search.travel_search_service.repository"])
class TravelSearchServiceApplication

fun main(args: Array<String>) {
	runApplication<TravelSearchServiceApplication>(*args)
}
