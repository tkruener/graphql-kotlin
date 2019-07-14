package com.expedia.graphql.generator.types

import com.expedia.graphql.TopLevelObject
import com.expedia.graphql.exceptions.InvalidSubscriptionTypeException
import com.expedia.graphql.generator.SchemaGenerator
import com.expedia.graphql.generator.TypeBuilder
import com.expedia.graphql.generator.extensions.getValidTopLevelFunctions
import com.expedia.graphql.generator.extensions.isNotPublic
import com.expedia.graphql.hooks.TopLevelType
import graphql.schema.GraphQLObjectType

internal class SubscriptionBuilder(generator: SchemaGenerator) : TypeBuilder(generator) {

    internal fun getSubscriptionObject(subscriptions: List<TopLevelObject>): GraphQLObjectType? {

        if (subscriptions.isEmpty()) {
            return null
        }

        val subscriptionBuilder = GraphQLObjectType.Builder()
        subscriptionBuilder.name(config.topLevelNames.subscription)

        for (subscription in subscriptions) {
            if (subscription.kClass.isNotPublic()) {
                throw InvalidSubscriptionTypeException(subscription.kClass)
            }

            subscription.kClass.getValidTopLevelFunctions(config.hooks, TopLevelType.Subscription)
                .forEach {
                    val function = generator.function(it, config.topLevelNames.subscription, subscription.obj)
                    val functionFromHook = config.hooks.didGenerateSubscriptionType(it, function)
                    subscriptionBuilder.field(functionFromHook)
                }
        }

        return subscriptionBuilder.build()
    }
}
