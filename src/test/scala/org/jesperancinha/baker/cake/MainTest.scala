package org.jesperancinha.baker.cake

import akka.actor.ActorSystem
import com.ing.baker.compiler.RecipeCompiler
import com.ing.baker.runtime.scaladsl.{Baker, EventInstance}
import org.jesperancinha.baker.cake.Recipes._
import org.scalatest.Matchers
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class MainTest extends AnyFlatSpec with Matchers {

  implicit val actorSystem = ActorSystem("peixinhosDaHorta")
  implicit val timeout: FiniteDuration = 2.seconds

  "A peixinhos da horta recipe" should "compile and validate" in {
    val compileRecipe = RecipeCompiler.compileRecipe(peixinhosDaHortaRecipe)

    compileRecipe.validationErrors should be('empty)

    println(compileRecipe.getRecipeVisualization);
  }

  "Baking a peixinhos da horta recipe" should "bake it" in {
    val compileRecipe = RecipeCompiler.compileRecipe(peixinhosDaHortaRecipe)

    compileRecipe.validationErrors should be('empty)

    implicit val actorSystem: ActorSystem =
      ActorSystem("PeixinhosDaHortaSystem")

    val baker: Baker = Baker.akkaLocalDefault(actorSystem)

    val program: Future[Unit] = for {
      _ <- baker.addInteractionInstances(Seq(
        setupCookingTableInstance, cookBeansInstance, cutPodsInHalfInstance, washBeansInstance,
        drainBeansInstance, fryPodsInstance, passPodsThroughBatterInstance, makeBatterInstance,
        seasonBatterInstance, removeBeanThreadInstance, addColdWaterInstance))
      recipeId <- baker.addRecipe(compileRecipe)
      recipeInstanceId = "my-id"
      _ <- baker.bake(recipeId, recipeInstanceId)
    } yield ()
    val unit: Unit = Await.result(program, 5 seconds)
    baker.fireEventAndResolveWhenCompleted("my-id", EventInstance.unsafeFrom(Recipes.familyIsHungry))
    baker.fireEventAndResolveWhenCompleted("my-id", EventInstance.unsafeFrom(Recipes.dinnerTime))
    Thread.sleep(5000)
    val completeGraph = Await.result(baker.getVisualState("my-id"), 5 seconds)

    println(unit)
    println(completeGraph)
  }
}