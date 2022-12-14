package com.codingame.codealamode.resolvers

import com.codingame.codealamode.TestUtils.Companion.action
import com.codingame.codealamode.TestUtils.Companion.gameState
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class BestActionResolverTest {

    @ParameterizedTest
    @CsvSource(
        "ligue2/game-2362403142607370200-state-1.txt, USE 8 3, Use STRAWBERRIES_CRATE",
        "ligue2/game-2362403142607370200-state-7.txt, USE 3 0, Recipe completed ?!", // On s'est retrouvé dans un état incohérent en plein assemblage
        "ligue2/game-2362403142607370200-state-45.txt, USE 8 4, \"Got some CHOPPED_STRAWBERRIES on table Table(position=8 4, item=Item(name=CHOPPED_STRAWBERRIES))\"",
        "ligue3/game-7942577706886182900-state-28.txt, USE 9 0, Use STRAWBERRIES_CRATE",
        "ligue3/game-7942577706886182900-state-88.txt, USE 0 3, Drop item to get CROISSANT before it burns!",
        "ligue3/game-7942577706886182900-state-193.txt, USE 0 4, Drop item",
        "ligueBronze/game-7942577706886182900-state-5.txt, USE 2 3, Use CHOPPING_BOARD",
        "ligueBronze/game-7942577706886182900-state-13.txt, USE 10 5, Use BLUEBERRIES_CRATE",
        "ligueBronze/game-7942577706886182900-state-33.txt, USE 0 5, Use OVEN", // On retire la tarte cuite du four
        "ligueBronze/game-7942577706886182900-state-121.txt, USE 5 0, Use DISHWASHER",
        "ligueBronze/game-7942577706886182900-state-125.txt, USE 0 2, Use ICE_CREAM_CRATE",
        "ligueBronze/game-7942577706886182900-state-129.txt, USE 2 4, \"Got some TART on table Table(position=2 4, item=Item(name=TART))\"",
        "ligueBronze/game--8170461584516249600-state-37.txt, USE 3 4, \"Got some DISH-BLUEBERRIES-ICE_CREAM-CHOPPED_STRAWBERRIES on table Table(position=3 4, item=Item(name=DISH-BLUEBERRIES-ICE_CREAM-CHOPPED_STRAWBERRIES))\"", // On récupère l'assiette déjà existante qui est compatible avec le 3e client
        "ligueBronze/game--2553030406430916600-state-219.txt, USE 0 1, Drop item", // Comme le four est déjà occupé et qu'on n'a plus aucun client qui ne nécessite pas d'utiliser le four, on dépose la pâte à côté du four
        "ligueBronze/game--3228865474660574200-state-35.txt, USE 4 0, Drop item to get STRAWBERRIES", // Comme le four est déjà occupé pour 2 clients, on dépose son croissant pour aller découper des fraises pour le dernier client
        "ligueBronze/game--5458706346828992500-state-33.txt, USE 8 2, Drop item to get DOUGH", // On va devoir se déplacer jusqu'à la 1ère table de libre la plus proche
        "ligueBronze/game--501847471512625220-state-7.txt, USE 2 6, Use DOUGH_CRATE", // On a le temps d'aller préparer la tarte
        "ligueBronze/game--501847471512625220-state-17.txt, USE 9 0, Use BLUEBERRIES_CRATE", // On est toujours le plus rapide pour faire une tarte
        "ligueBronze/game--501847471512625220-state-21.txt, USE 8 2, Drop item to get DOUGH", // On va devoir se déplacer jusqu'à la 1ère table de libre la plus proche
        "ligueBronze/game--501847471512625220-state-55.txt, USE 8 4, Drop item because cannot use STRAWBERRIES provider", // On doit lâcher son assiette pour préparer des fraises
        "ligueBronze/game--2174831961734777090-state-20.txt, USE 8 4, Drop item because cannot use STRAWBERRIES provider", // On doit lâcher son assiette pour préparer des fraises
        "ligueBronze/game-3826859358225928200-state-45.txt, USE 5 2,", // On prend l'assiette pour aider à servir le 3e client
        "ligueBronze/game-3826859358225928200-state-47.txt, USE 10 4,", // Comme il ne reste plus qu'à attendre que le croissant cuise, on s'en rapproche pour servir le 3e client
        "ligueBronze/game-3826859358225928200-state-49.txt, USE 8 2,", // On dépose l'assiette sur la table la plus proche de nous et du partenaire pour l'aider à servir le 3e client
        quoteCharacter = '"',
    )
    fun resolveBestAction(gameStatePath: String, expectedActionString: String, expectedActionComment: String?) {
        val gameState = gameState(gameStatePath)
        val bestActionResolver = BestActionResolver()

        val action = bestActionResolver.resolveBestActionFrom(gameState)

        val expectedAction = action(expectedActionString)
        assertThat(action).isEqualTo(expectedAction)
        if (StringUtils.isNoneBlank(expectedActionComment)) {
            assertThat(action.comment).isEqualTo(expectedActionComment)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "ligue2/game-2362403142607370200-state-1.txt",
    )
    fun resolveBestActionFastEnough(gameStatePath: String) {
        val currentTimestamp = System.currentTimeMillis()

        val gameState = gameState(gameStatePath)
        val bestActionResolver = BestActionResolver()

        bestActionResolver.resolveBestActionFrom(gameState)

        assertThat(System.currentTimeMillis())
            .`as`("Maximum response time is <= 1 second")
            .isLessThanOrEqualTo(currentTimestamp + 1000)
    }

}
