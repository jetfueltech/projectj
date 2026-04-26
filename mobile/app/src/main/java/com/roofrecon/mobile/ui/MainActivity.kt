package com.roofrecon.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.roofrecon.mobile.auth.SessionStore
import com.roofrecon.mobile.net.BackendClient
import com.roofrecon.mobile.net.Job
import com.roofrecon.mobile.net.LoginRequest
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Scaffold { p -> Root(Modifier.padding(p)) } } }
    }
}

@Composable
private fun Root(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var token by remember { mutableStateOf(SessionStore.token(ctx)) }
    if (token == null) {
        LoginScreen(modifier) { saved ->
            SessionStore.save(ctx, saved.first, saved.second)
            token = saved.first
        }
    } else {
        JobsScreen(modifier, token!!)
    }
}

@Composable
private fun LoginScreen(modifier: Modifier, onLogin: (Pair<String, String>) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Roof Recon", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
        )
        Button(
            onClick = {
                scope.launch {
                    try {
                        val res = BackendClient.api.login(LoginRequest(email, password))
                        onLogin(res.token to res.email)
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign in") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun JobsScreen(modifier: Modifier, token: String) {
    val ctx = LocalContext.current
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        try {
            jobs = BackendClient.api.listJobs(BackendClient.bearer(token)).jobs
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Inspections", style = MaterialTheme.typography.titleLarge)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs, key = { it.id }) { job ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Job ${job.id.take(8)}")
                        Text(job.status, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            ctx.startActivity(
                                Intent(ctx, JobActivity::class.java)
                                    .putExtra(JobActivity.EXTRA_JOB_ID, job.id),
                            )
                        }) { Text("Open") }
                    }
                }
            }
        }
    }
}
