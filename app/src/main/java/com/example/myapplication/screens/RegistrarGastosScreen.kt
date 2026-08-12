package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.compose.AsyncImagePainter
import com.example.myapplication.model.Gasto

@Composable
fun RegistrarGastosScreen() {

    val context = LocalContext.current

    // Datos de ejemplo en memoria.
    // Cada gasto tiene un ID único y estable.
    val gastos = listOf(
        Gasto(
            1,
            "Pizzas",
            "Santi",
            4500,
            "9:45 PM",
            "https://picsum.photos/seed/pizza/200"
        ),
        Gasto(
            2,
            "Cervezas",
            "Vale",
            5800,
            "10:12 PM",
            "https://picsum.photos/seed/drinks/200"
        ),
        Gasto(
            3,
            "Postre",
            "Marcos",
            2100,
            "11:05 PM",
            "https://picsum.photos/seed/dessert/200"
        ),
        Gasto(
            4,
            "Hamburguesas",
            "Santi",
            3200,
            "8:30 PM",
            "https://picsum.photos/seed/burger/200"
        ),
        Gasto(
            5,
            "Tacos",
            "Mateo",
            1800,
            "8:45 PM",
            "https://picsum.photos/seed/tacos/200"
        ),
        Gasto(
            6,
            "Bebidas",
            "Sofía",
            1200,
            "9:00 PM",
            "https://picsum.photos/seed/soda/200"
        ),
        Gasto(
            7,
            "Uber",
            "Vale",
            2500,
            "11:30 PM",
            "https://picsum.photos/seed/car/200"
        ),
        Gasto(
            8,
            "Entradas",
            "Marcos",
            3000,
            "7:30 PM",
            "https://picsum.photos/seed/tickets/200"
        ),
        Gasto(
            9,
            "Snacks",
            "Mateo",
            950,
            "10:30 PM",
            "https://picsum.photos/seed/snacks/200"
        ),
        Gasto(
            10,
            "Café",
            "Sofía",
            750,
            "11:45 PM",
            "https://picsum.photos/seed/coffee/200"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
    ) {

        // Título de la pantalla
        Text(
            text = "Pizza & Birra",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Información general
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD44329)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Total Gastado",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Q12,400",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFAA66)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "6 personas",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Hoy, 21:30",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            text = "DETALLE DE GASTOS",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 8.dp
            )
        )

        // LazyColumn requerida por el laboratorio
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = gastos,

                // ID único y estable.
                // No utilizamos el índice de la lista.
                key = { gasto -> gasto.id }
            ) { gasto ->

                GastoCard(
                    gasto = gasto,
                    onClick = {
                        Toast.makeText(
                            context,
                            "Seleccionaste ${gasto.nombre}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}


@Composable
fun GastoCard(
    gasto: Gasto,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE4F4FA)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Imagen cargada con Coil
            // Mientras carga, muestra un placeholder.
            SubcomposeAsyncImage(
                model = gasto.imageUrl,
                contentDescription = "Imagen de ${gasto.nombre}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            ) {

                when (painter.state) {

                    is AsyncImagePainter.State.Loading -> {

                        ImagePlaceholder(
                            texto = gasto.nombre.take(1)
                        )
                    }

                    is AsyncImagePainter.State.Error -> {

                        ImagePlaceholder(
                            texto = gasto.nombre.take(1)
                        )
                    }

                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }

            // Nombre y persona que pagó
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = gasto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pagó: ${gasto.pagador}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Precio y hora
            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = "Q${gasto.monto}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = gasto.hora,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
fun ImagePlaceholder(
    texto: String
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFB4A6)),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD44329)
        )
    }
}