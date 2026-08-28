package com.prugo8.pagame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prugo8.pagame.model.ParticipantBalance
import com.prugo8.pagame.model.Settlement
import com.prugo8.pagame.ui.components.PagameBottomNavigation
import com.prugo8.pagame.viewmodel.PagameViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(
    eventId: String,
    viewModel: PagameViewModel,
    onBack: () -> Unit,
    onSettleClick: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    val event = events.find { it.id == eventId } ?: events.firstOrNull()

    if (event == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Evento no encontrado")
        }
        return
    }

    val balances = remember(event) { viewModel.getBalances(eventId) }
    val settlements = remember(event) { viewModel.getSettlements(eventId) }
    val userBalance = balances.find { it.participant.isUser }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Balances: ${event.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = { PagameBottomNavigation(selectedTab = 2) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("RESUMEN DE BALANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Balance Gradient Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFA93017), Color(0xFFCB482C))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Text("Tu balance neto", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                                val net = userBalance?.netBalance ?: 0.0
                                val netFormatted = if (net >= 0) "+${event.currency}${"%,.2f".format(net)}" else "-${event.currency}${"%,.2f".format(abs(net))}"
                                
                                Text(netFormatted, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)

                                Divider(
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Pagaste en total", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        Text("${event.currency}${"%,.2f".format(userBalance?.totalPaid ?: 0.0)}", color = Color(0xFFFFDAD3), fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Tu consumo", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        Text("${event.currency}${"%,.2f".format(userBalance?.totalOwed ?: 0.0)}", color = Color(0xFFFFDCC4), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Estado por Participante", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // Grid Participantes
                items(balances) { pb ->
                    ParticipantBalanceRow(pb = pb, currency = event.currency)
                }

                item {
                    Text("Sugerencias de Pago Óptimas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // Lista de pagos sugeridos
                items(settlements) { s ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F6FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.fromParticipantName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(s.toParticipantName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text(
                                "${event.currency}${"%,.2f".format(s.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bottom CTA
            Button(
                onClick = onSettleClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Liquidar y Saldar Cuentas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ParticipantBalanceRow(pb: ParticipantBalance, currency: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F6FF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(pb.participant.avatarInitials, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(pb.participant.name, fontWeight = FontWeight.SemiBold)
            }

            val isPositive = pb.netBalance >= 0
            val label = if (isPositive) "Le deben $currency${"%,.2f".format(pb.netBalance)}" else "Debe $currency${"%,.2f".format(abs(pb.netBalance))}"
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
    }
}
