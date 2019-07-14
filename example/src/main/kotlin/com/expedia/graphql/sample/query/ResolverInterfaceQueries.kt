package com.expedia.graphql.sample.query

import com.expedia.graphql.sample.resolvers.Query
import com.expedia.graphql.sample.resolvers.Resolver
import org.springframework.stereotype.Component

interface Vehicle<X : Vehicle<X>> : VehicleResolverI<X> {
	fun move(meters: Int): String

	val color: String
}

interface VehicleResolverI<X : Vehicle<X>> {
	fun customResolver(self: X): String = error("Should be implemented by the resolvers")
}

class Sportscar : Vehicle<Sportscar> {

	override fun move(meters: Int): String = "Zshshhhh"

	override val color: String = "Red"
}

class Truck : Vehicle<Truck> {
	override fun move(meters: Int): String = "Tuck tuck"

	override val color: String = "Yellow"
}

@Component
class VehicleResolver : Resolver<Vehicle<*>>() {

	@Query
	fun vehicle(wantATruck: Boolean) = if (wantATruck) Truck() else Sportscar()

	fun doStuff() = "qweqwe"
}

@Component
class TruckResolver : Resolver<Truck>(), VehicleResolverI<Truck> {

	fun doSomeExtraNoise() = "RATATA"

	override fun customResolver(self: Truck): String = "Truck resolver"
}

@Component
class SportscarResolver : Resolver<Sportscar>(), VehicleResolverI<Sportscar> {
	override fun customResolver(self: Sportscar): String = "Sportscar resolver"
}
