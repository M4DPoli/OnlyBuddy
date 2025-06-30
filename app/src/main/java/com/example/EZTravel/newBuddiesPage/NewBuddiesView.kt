package com.example.EZTravel.newBuddiesPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.R
import com.example.EZTravel.travelPage.Application
import com.example.EZTravel.travelPage.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun NewBuddiesView(
    n: Int,
    onBack: () -> Unit,
    onSnack: (String) -> Job,
    vm: NewBuddiesViewModel = hiltViewModel()
) {
    vm.initBuddies(n)

    val scope = rememberCoroutineScope()

    val isLoading by vm.isLoading.collectAsState()
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    else{
        Scaffold(
            topBar = {
                TopAppBar(onBack)
            },
            bottomBar = {
                BottomBar(n, onBack, scope,onSnack)
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(n - 1) { i ->
                    Text(
                        text = "Buddy ${i + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                    )

                    OutlinedTextField(
                        value = vm.buddies_name[i],
                        onValueChange = {
                            vm.buddies_name[i] = it
                        },
                        label = { Text("Name") },
                        isError = vm.errors_name[i],
                        supportingText = {
                            if (vm.errors_name[i]) {
                                Text("Mandatory Field")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )

                    OutlinedTextField(
                        value = vm.buddies_surname[i],
                        onValueChange = { vm.buddies_surname[i] = it },
                        label = { Text("Surname") },
                        isError = vm.errors_surname[i],
                        supportingText = {
                            if (vm.errors_surname[i]) {
                                Text("Mandatory Field")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                    )
                }

            }

        }
    }

}

@Composable
fun BottomBar(n: Int, onBack: () -> Unit, scope : CoroutineScope, onSnack: (String) -> Job, vm: NewBuddiesViewModel = hiltViewModel()) {

    BottomAppBar(
        containerColor = AppBarColors.BottomBarContainerColor(),
        contentColor = AppBarColors.BottomBarContentColor(),
        tonalElevation = AppBarColors.BottomBarTonalElevation,
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (vm.validate(n)) {
                        scope.launch {
                            vm.toggleIsLoading()
                            val success = vm.addNewApplications(
                                application = Application(
                                    id = UUID.randomUUID().toString(),
                                    user = null,
                                    state = State.PENDING.ordinal,
                                    size = n,
                                    buddies = vm.getFullNames()
                                )
                            )
                            if (success) onBack()
                            else {onSnack("Problems on adding new application");vm.toggleIsLoading()}
                        }
                    }
                },
                modifier = Modifier
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .size(18.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_small),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(end = 24.dp),
                    text = stringResource(R.string.button_save)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    onBack: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { Text("Extra Buddies") }, navigationIcon = {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back"
                )
            }
        },
        colors = AppBarColors.TopAppBarColor()
    )
}