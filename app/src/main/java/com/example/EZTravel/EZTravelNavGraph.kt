package com.example.EZTravel

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.EZTravel.userProfile.UserProfileEdit
import com.example.EZTravel.applicationsPage.ApplicationsView
import com.example.EZTravel.chat.ChatListView
import com.example.EZTravel.chat.ChatView
import com.example.EZTravel.explore.ExploreView
import com.example.EZTravel.homePage.HomePageView
import com.example.EZTravel.login.LoginView
import com.example.EZTravel.login.RegistrationView
import com.example.EZTravel.newBuddiesPage.NewBuddiesView
import com.example.EZTravel.newTravelPage.NewTravelView
import com.example.EZTravel.notification.page.NotificationPageView
import com.example.EZTravel.photoGallery.GalleryView
import com.example.EZTravel.searchImportPage.SearchImportView
import com.example.EZTravel.travelPage.TravelView
import com.example.EZTravel.tripReviewPage.TripReviewView
import com.example.EZTravel.userProfile.UserProfileView
import kotlinx.coroutines.launch


object DefaultTransitions {

    val stiffness = Spring.StiffnessMediumLow
    val dampingRatio = Spring.DampingRatioNoBouncy

    val navBarEntries = listOf(
        EZTravelDestinations.LOGIN_ROUTE,
        EZTravelDestinations.USER_ROUTE,
        EZTravelDestinations.ADD_TRAVEL_ROUTE,
        EZTravelDestinations.EXPLORE_ROUTE,
        EZTravelDestinations.HOME_ROUTE,
        EZTravelDestinations.CHAT_LIST_ROUTE
    )


    val duration = 200

    //Homepage transitions

    val enterTransition : AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            val from = initialState.destination.route
            val to = targetState.destination.route




            when {
                to in navBarEntries && from in navBarEntries -> {
                    slideInVertically (
                        initialOffsetY = {fullHeight -> fullHeight/6}
                    ) +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = duration
                                ),
                            )
                }

                to in navBarEntries && from !in navBarEntries -> {
                    slideInHorizontally(
                        initialOffsetX = {fullWidth ->  fullWidth},
                        animationSpec = spring(
                            dampingRatio = dampingRatio,
                            stiffness = stiffness
                        )
                    ) + fadeIn(animationSpec = tween(duration))
                }

                else -> {
                    slideInHorizontally(
                        initialOffsetX = {fullWidth ->  fullWidth},
                        animationSpec = spring(
                            dampingRatio = dampingRatio,
                            stiffness = stiffness
                        )
                    ) //+ fadeIn(animationSpec = tween(duration))
                }
            }
        }

    val exitTransition : AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val from = initialState.destination.route
        val to = targetState.destination.route

        when{
            to in navBarEntries && from in navBarEntries -> {
                slideOutVertically (
                    targetOffsetY = {fullHeight -> fullHeight/6}
                ) +
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = duration
                            ),
                        )
            }
            to in navBarEntries && from !in navBarEntries -> {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = spring(
                        dampingRatio = dampingRatio,
                        stiffness = stiffness
                    )
                ) //+ fadeOut(animationSpec = tween(duration))
            }
            else -> {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = spring(
                        dampingRatio = dampingRatio,
                        stiffness = stiffness
                    )
                )// + fadeOut(animationSpec = tween(duration))
            }
        }
    }

    val enterPopTransition : AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            val from = initialState.destination.route
            val to = targetState.destination.route

            when {
                to in navBarEntries && from in navBarEntries -> {
                    slideInVertically (
                        initialOffsetY = {fullHeight -> fullHeight/6}
                    ) +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = duration
                                ),
                            )
                }

                to in navBarEntries && from !in navBarEntries -> {
                    slideInHorizontally(
                        initialOffsetX = {fullWidth ->  -fullWidth/4},
                        animationSpec = spring(
                            dampingRatio = dampingRatio,
                            stiffness = stiffness
                        )
                    ) //+ fadeIn(animationSpec = tween(duration))
                }

                else -> {
                    slideInHorizontally(
                        initialOffsetX = {fullWidth ->  -fullWidth/4},
                        animationSpec = spring(
                            dampingRatio = dampingRatio,
                            stiffness = stiffness
                        )
                    )// + fadeIn(animationSpec = tween(duration))
                }
            }
        }


    val exitPopTransition : AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val from = initialState.destination.route
        val to = targetState.destination.route

        when{
            to in navBarEntries && from in navBarEntries -> {
                slideOutVertically (
                    targetOffsetY = {fullHeight -> fullHeight/6}
                ) +
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = duration
                            ),
                        )
            }
            to in navBarEntries && from !in navBarEntries -> {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth},
                    animationSpec = spring(
                        dampingRatio = dampingRatio,
                        stiffness = stiffness
                    )
                ) //+ fadeOut(animationSpec = tween(duration))
            }
            else -> {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth},
                    animationSpec = spring(
                        dampingRatio = dampingRatio,
                        stiffness = stiffness
                    )
                ) //+ fadeOut(animationSpec = tween(duration))
            }
        }
    }

}


@Composable
fun EZTravelNavGraph(
    launchCredentialManager : () -> Unit,
    startDestination: String,
    signOut : () -> Unit,
    destinationToReach: String?,
    navController: NavHostController = rememberNavController(),
    navActions: EzTravelNavigationActions = remember(navController) {
        EzTravelNavigationActions(navController)
    }
) {
    val bottomBarDestinations = listOf("home", "explore", "user", "chatList")
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onSnack = {s : String -> scope.launch{snackbarHostState.showSnackbar(s)}}
    val hasNavigatedToInitialDestination = remember { mutableStateOf(false) }

    LaunchedEffect(destinationToReach, hasNavigatedToInitialDestination.value) {
        if (!destinationToReach.isNullOrEmpty()
            && destinationToReach != EZTravelDestinations.HOME_ROUTE
            && !hasNavigatedToInitialDestination.value
        ) {
            navController.navigate(destinationToReach) {
                popUpTo(EZTravelDestinations.HOME_ROUTE) { inclusive = false }
            }
            hasNavigatedToInitialDestination.value = true
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        bottomBar = {

            if (currentDestination?.route in bottomBarDestinations) {
                NavBar(
                    navController,
                    onHome = navActions.navigateHome,
                    onExplore = navActions.navigateExplore,
                    onCreateTravelNew = navActions.navigateAddTravel,
                    onUser = navActions.navigateUser,
                    onChatList = navActions.navigateChatList,
                    onLogin = navActions.navigateLogin
                )
            }

        }
    ) { outerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(
                EZTravelDestinations.HOME_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {
                HomePageView(
                    navActions.navigateTravel,
                    {s : String -> onSnack(s)},
                    outerPadding
                )
            }
            composable(
                EZTravelDestinations.EXPLORE_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {
                ExploreView(
                    navActions.navigateTravel,
                    outerPadding
                )
            }
            composable(
                EZTravelDestinations.SEARCH_IMPORT_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) { SearchImportView(navActions.navigateCloneTravel,navActions.navigateBack) }

            composable(
                EZTravelDestinations.OTHER_USER_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.USER_ID) { type = NavType.StringType }
                )) {
                UserProfileView(
                    navActions.navigateBack,
                    navActions.navigateTravel,
                    navActions.navigateEditUser,
                    navActions.navigateNotifications,
                    { },
                    outerPadding
                )
            }
            composable(
                EZTravelDestinations.USER_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {

                UserProfileView(
                    navActions.navigateBack,
                    navActions.navigateTravel,
                    navActions.navigateEditUser,
                    navActions.navigateNotifications,
                    signOut,
                    outerPadding
                )
            }
            composable(
                EZTravelDestinations.EDIT_PROFILE_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {
                UserProfileEdit(
                    navActions.navigateBack,
                    {s : String -> onSnack(s)}
                )
            }
            composable(
                EZTravelDestinations.TRAVEL_ROUTE,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType },
                ),
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) { entry ->
                TravelView(
                    navActions.navigateBack,
                    navActions.navigateApplications,
                    navActions.navigateBuddies,
                    navActions.navigateCloneTravel,
                    navActions.navigateCreateTravelWithId,
                    navActions.navigatePhotos,
                    navActions.navigateOtherUser,
                    navActions.navigateReview,
                    navActions.navigateLogin,
                    {s: String ->
                        scope.launch { snackbarHostState.showSnackbar(s) }
                    }
                )
            }
            composable(
                EZTravelDestinations.APPLICATIONS_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType }
                )) { entry ->
                ApplicationsView(
                    navActions.navigateBack,
                    navActions.navigateOtherUser,
                    {s: String ->
                        scope.launch { snackbarHostState.showSnackbar(s) }
                    }
                )
            }
            composable(
                EZTravelDestinations.CREATE_ROUTE_WITH_ID,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType }
                )) { entry ->
                NewTravelView(
                    navActions.navigateBack,
                    navActions.navigateHome,
                    {s -> onSnack(s)},
                    false
                )
            }
            composable(
                EZTravelDestinations.CREATE_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {
                NewTravelView(
                    onBack = navActions.navigateBack,
                    navActions.navigateHome,
                    {s -> onSnack(s)},
                    true
                )
            }
            composable(
                EZTravelDestinations.CLONE_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType }
                )) { entry ->
                NewTravelView(
                    navActions.navigateBack,
                    navActions.navigateHome,
                    {s -> onSnack(s)},
                    true
                )
            }
            composable(
                EZTravelDestinations.BUDDIES_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType },
                    navArgument(EZTravelDestinationsArgs.N_BUDDIES) { type = NavType.IntType }
                )) { entry ->
                NewBuddiesView(
                    n = entry.arguments?.getInt(EZTravelDestinationsArgs.N_BUDDIES)!!,
                    navActions.navigateBack,
                    {s: String ->
                        scope.launch { snackbarHostState.showSnackbar(s) }
                    }
                )
            }
            composable(
                EZTravelDestinations.PHOTOS_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType }
                )) { entry ->
                GalleryView(
                    navActions.navigateBack
                )
            }
            composable(
                EZTravelDestinations.REVIEW_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.TRAVEL_ID) { type = NavType.StringType}
                )) { entry ->
                TripReviewView(
                    navActions.navigateBack,
                    {s -> onSnack(s)}
                )

            }
            composable(
                EZTravelDestinations.NOTIFICATIONS_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {
                NotificationPageView(
                    navActions.navigateApplications,
                    navActions.navigateHome,
                    navActions.navigateTravel,
                    navActions.navigateBack
                )
            }
            composable(
                EZTravelDestinations.LOGIN_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf())
                {
                    LoginView (
                        navActions.navigateHome,
                        launchCredentialManager,
                        navActions.navigateRegistration
                        )
                }
            composable(
                EZTravelDestinations.REGISTRATION_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition)
                {
                    RegistrationView(
                        navActions.navigateHome
                    )
                }

            composable(
                EZTravelDestinations.CHAT_LIST_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition
            ) {
                ChatListView(
                    onChatClick = navActions.navigateChat,
                )
            }

            composable(
                EZTravelDestinations.CHAT_ROUTE,
                enterTransition = DefaultTransitions.enterTransition,
                exitTransition = DefaultTransitions.exitTransition,
                popExitTransition = DefaultTransitions.exitPopTransition,
                popEnterTransition = DefaultTransitions.enterPopTransition,
                arguments = listOf(
                    navArgument(EZTravelDestinationsArgs.CHAT_ID) { type = NavType.StringType }
                )
            ) {
                ChatView(
                    onBack = navActions.navigateBack,
                    onSnack = {s -> onSnack(s)}
                )
            }


            dialog(EZTravelDestinations.ADD_TRAVEL_ROUTE) {
                NewTripModal(
                    navActions.navigateCreateTravelNew,
                    navActions.navigateSearchImport,
                    navActions.navigateBack
                )
            }
        }
    }
}
