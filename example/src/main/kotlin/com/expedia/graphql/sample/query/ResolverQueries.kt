package com.expedia.graphql.sample.query

import com.expedia.graphql.annotations.GraphQLIgnore
import com.expedia.graphql.sample.resolvers.Mutation
import com.expedia.graphql.sample.resolvers.Parent
import com.expedia.graphql.sample.resolvers.Query
import com.expedia.graphql.sample.resolvers.Resolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import kotlin.random.Random

data class Item(val id: String, val name: String = "Item with id $id")

data class ItemReview(val title: String,
					  val comment: String,
					  @GraphQLIgnore
					  val userId: String = "abc-${Random.nextInt()}")

data class User(val id: String, val name: String)

@Service
class ItemService {

	fun findItemById(id: String) = Item(id)

	fun findSimilarItems(id: String) = listOf(Item("$id qwe"), Item("foo $id "), Item("$id qweqwe"))
}

@Service
class ReviewService {

	fun findReviewsForItem(itemId: String) = (0..3).map {
		ItemReview("Review ${Random.nextInt()}", "Review ${Random.nextInt()}")
	}

	fun postReview(itemId: String, itemReview: ItemReview) = itemReview
}

@Service
class UserService {
	fun findUserById(userId: String) = User(userId, "John Doe")
}

@Component
class ItemResolver : Resolver<Item>() {

	@Autowired
	private lateinit var itemService: ItemService

	@Query
	fun item(id: String) = itemService.findItemById(id)

	fun similars(item: Item) = itemService.findSimilarItems(item.id)
}

@Component
class ReviewResolver : Resolver<ItemReview>() {

	@Autowired
	private lateinit var reviewService: ReviewService

	fun reviews(@Parent parent: Item) = reviewService.findReviewsForItem(parent.id)

	@Mutation
	fun userReview(itemId: String, review: ItemReview) = reviewService.postReview(itemId, review)
}

@Component
class UserResolver : Resolver<User>() {

	@Autowired
	private lateinit var userService: UserService

	@Query
	fun user(userId: String) = userService.findUserById(userId)

	fun user(@Parent review: ItemReview) = userService.findUserById(review.userId)

	fun favoriteSong() = "Here comes the sun"
}

@Component
class SecondaryUserResolver : Resolver<User>() {

	fun favoriteColor() = "blue"
}