package com.example.bikerentalapp
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.children
import androidx.lifecycle.get
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import kotlinx.coroutines.tasks.await
import kotlin.collections.getValue
import coil.compose.rememberAsyncImagePainter


suspend fun getBikesFromFirebase(): List<Bike> {
    Log.d("Firebase", "Starting to fetch bikes...")
    val database = Firebase.database("https://bikerentalapp-abe51-default-rtdb.europe-west1.firebasedatabase.app").reference
    val bikesRef = database.child("bikes")
    val dataSnapshot = bikesRef.get().await()
    Log.d("Firebase", "Data fetched: ${dataSnapshot.value}")
    val bikesList = mutableListOf<Bike>()

    for (bikeSnapshot in dataSnapshot.children) {
        Log.d("Firebase", "Processing bike: ${bikeSnapshot.key}")
        val bike = bikeSnapshot.getValue(Bike::class.java)
        Log.d("Firebase", "Bike object: $bike")
        bike?.let { bikesList.add(it) }
    }
    Log.d("Firebase", "Total bikes fetched: ${bikesList.size}")
    return bikesList
}

data class Bike(
    val name: String = "",
    val description: String = "",
    val imageRes: String = "",
    val details: String = "",
    val pricePerHour: Double = 0.0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BikeRentalApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeRentalApp() {
    var showCart by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showVerifyOrderDialog by remember { mutableStateOf(false) }
    var isFromCart by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var bikes by remember { mutableStateOf<List<Bike>>(emptyList()) }

    LaunchedEffect(key1 = true) {
        bikes = getBikesFromFirebase()
    }
    var selectedBike by remember { mutableStateOf<Bike?>(null) }
    val cartItems = remember { mutableStateListOf<Bike>() }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bike Rental") },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_icon),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(40.dp)
                    )
                },
                actions = {
                    Spacer(modifier = Modifier.width(5.dp))

                    IconButton(onClick = { showSearchDialog = true }) { // Add search icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search",
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    IconButton(onClick = { showCart = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.shopping_cart),
                            contentDescription = "Cart"
                        )
                    }
                },
                colors = androidx . compose . material3 . TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.cart_background),
                titleContentColor = colorResource(id = R.color.black),
                navigationIconContentColor = colorResource(id = R.color.white),
                actionIconContentColor = colorResource(id = R.color.black)
            )
            )

        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Image(
                painter = painterResource(id = R.drawable.background_image1),
                contentDescription = "Background Image 1",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                alpha = 0.6f
            )
            Row(modifier = Modifier.fillMaxSize()) {
                if (selectedBike == null && !showCart && !showDetailsDialog && !showSearchDialog) {
                    HomeScreen(onBikeClick = {
                        selectedBike = it
                        isFromCart = false
                    })
                }

                if (selectedBike != null) {
                    BikeDetailScreen(
                        bike = selectedBike!!,
                        onAddToCart = {
                            cartItems.add(selectedBike!!)
                            selectedBike = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Bike added to cart!")
                            }
                        },
                        onClose = { selectedBike = null
                            if (!isSearchActive) {
                                showSearchDialog = false
                            }},
                        isFromCart = isFromCart
                    )
                }

                if (showCart) {
                    DialogOverlay {
                        CartScreen(
                            cartItems = cartItems,
                            onRemoveItem = { bikeToRemove ->
                                cartItems.remove(bikeToRemove)
                            },
                            onViewItem = { bike ->
                                selectedBike = bike
                                isFromCart = true
                            },
                            onClose = { showCart = false },
                            onOrderConfirmed = { orderName, orderAddress, orderPaymentMethod, cartItems ->
                                showVerifyOrderDialog = true
                                name = orderName
                                address = orderAddress
                                paymentMethod = orderPaymentMethod
                            }
                        )
                    }
                }

                if (showSearchDialog) {
                    isSearchActive = true
                    SearchDialog(
                        bikes = bikes,
                        onDismiss = {
                            showSearchDialog = false
                        },
                        onBikeClick = { bike ->
                            selectedBike = bike
                            isFromCart = false
                        }
                    )
                }

                if (showDetailsDialog && selectedBike != null) {
                    DialogOverlay {
                        BikeDetailScreen(
                            bike = selectedBike!!,
                            onAddToCart = {
                                cartItems.add(selectedBike!!)
                                showDetailsDialog = false
                            },
                            onClose = {
                                showDetailsDialog = false
                                if (isFromCart) {
                                    showCart = true
                                }
                            },
                            isFromCart = isFromCart
                        )
                    }
                }

                if (showVerifyOrderDialog) {
                    DialogOverlay {
                        VerifyOrderDialog(
                            name = name,
                            address = address,
                            paymentMethod = paymentMethod,
                            cartItems = cartItems,
                            onDismiss = { showVerifyOrderDialog = false },
                            onOrderVerified = {
                                showVerifyOrderDialog = false
                                showConfirmationDialog = true
                                showCart = false

                            }
                        )
                    }
                }


                if (showConfirmationDialog) {
                    ConfirmationDialog(message = "Thank you! Order has been confirmed!", onDismiss = {
                        showConfirmationDialog = false
                        cartItems.clear()
                    })
                }
            }
        }
    }
}

@Composable
fun DialogOverlay(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.6f)
        ) {}
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}


@Composable
fun HomeScreen(onBikeClick: (Bike) -> Unit) {
    var bikes by remember { mutableStateOf<List<Bike>>(emptyList()) }

    LaunchedEffect(key1 = true) {
        bikes = getBikesFromFirebase()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_image1),
            contentDescription = "Background Image 1",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .size(200.dp),
            contentScale = ContentScale.FillBounds,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            items(bikes) { bike ->
                BikeItem(bike = bike, onClick = { onBikeClick(bike) })
            }
        }
    }
}


@Composable
fun BikeItem(bike: Bike, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageId = context.resources.getIdentifier(bike.imageRes, "drawable", context.packageName)

    Card(modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .padding(5.dp)
        .clickable {
            Log.d("BikeItem", "Bike clicked: $bike")
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.background_card)))
    {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(5.dp)
                .clickable {
                    Log.d("BikeItem", "Bike clicked: $bike")
                    onClick()
                },
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.bike_item_background))
        ){
            Row(
                modifier = Modifier.padding(10.dp), // Smaller padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageId != 0) {
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = bike.name,
                        modifier = Modifier.size(130.dp)
                    )
                } else {
                    // Placeholder image or error handling
                    Image(
                        painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                        contentDescription = "Image not found",
                        modifier = Modifier.size(130.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp)) // Smaller spacer
                Column {
                    Text(bike.name, fontSize = 16.sp, fontWeight = FontWeight.Bold) // Smaller text
                    Text(bike.description, fontSize = 12.sp) // Smaller text
                }
            }
        }
    }
}


@Composable
fun BikeDetailScreen(bike: Bike, onAddToCart: () -> Unit, onClose: () -> Unit, isFromCart: Boolean) {
    Log.d("BikeDetailScreen", "Bike received: $bike")
    var showDetailsDialog by remember { mutableStateOf(false) }
    if (showDetailsDialog) {
        DetailsDialog(bike = bike, onDismiss = { showDetailsDialog = false })
    }

    val context = LocalContext.current
    val imageId = context.resources.getIdentifier(bike.imageRes, "drawable", context.packageName)

    Dialog(onDismissRequest = { onClose() }) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp)
                .border(3.dp, colorResource(id = R.color.black), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(25.dp)) {
                if (imageId != 0) {
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = bike.name,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Placeholder image or error handling
                    Image(
                        painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                        contentDescription = "Image not found",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(bike.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showDetailsDialog = true }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_dialog_info),
                            contentDescription = "Open details"
                        )
                    }
                }

                Text(bike.description, fontSize = 16.sp, color = androidx.compose.ui.graphics.Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                if (!isFromCart) {
                    Button(
                        onClick = onAddToCart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.cancel_button_text),
                            contentColor = colorResource(id = R.color.black)
                        )
                    ) {
                        Text("Add to Cart")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.white),
                        contentColor = colorResource(id = R.color.cancel_button_text)
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}


@Composable
fun DetailsDialog(bike: Bike, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bike Details", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = bike.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Opis: ${bike.description}")
                Text(text = bike.details)
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = "Price: ${bike.pricePerHour}$/hour")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss, modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.cancel_button_text),
                        contentColor = colorResource(id = R.color.black)
                    ),
                ) {
                    Text("Close")
                }
            }
        }
    }
}



@Composable
fun CartScreen(
    cartItems: List<Bike>,
    onRemoveItem: (Bike) -> Unit,
    onViewItem: (Bike) -> Unit,
    onClose: () -> Unit,
    onOrderConfirmed: (String, String, String, List<Bike>) -> Unit
) {
    var showCheckoutDialog by remember { mutableStateOf(false) }

    if (showCheckoutDialog) {
        CheckoutDialog(
            cartItems = cartItems,
            onDismiss = { showCheckoutDialog = false },
            onOrderVerified = { name, address, paymentMethod ->
                onOrderConfirmed(name, address, paymentMethod, cartItems)
                showCheckoutDialog = false
            }
        )
    }

    Dialog(onDismissRequest = { onClose() }) {
        Card(
            modifier = Modifier
                .border(
                    3.dp, colorResource(id = R.color.black),
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            val modifier = if (cartItems.isEmpty()) {
                Modifier
                    .padding(20.dp)
                    .size(230.dp)
            } else {
                Modifier
                    .padding(10.dp)
                    .size(450.dp)
            }
            Column(modifier = modifier.verticalScroll(rememberScrollState())) {
                if (cartItems.isEmpty()) {
                    Text(
                        "Your cart is empty",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp)
                            .padding(bottom = 50.dp)
                    )
                } else {
                    Text("Cart", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(cartItems) { bike ->
                            BikeItemCart(bike = bike, onRemoveItem = onRemoveItem, onViewItem = onViewItem)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val numOfItems = cartItems.count()
                    val totalPrice = cartItems.sumOf { it.pricePerHour }
                    Text(text = "Total: $totalPrice$/hour", fontWeight = FontWeight.Bold)
                    Text(text = "Items: $numOfItems")
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.End

                ) {
                    if (cartItems.isNotEmpty()) {
                        Button(
                            onClick = { showCheckoutDialog = true }, colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.cancel_button_text),
                                contentColor = colorResource(id = R.color.black)
                            )
                        ) {
                            Text("Continue")
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    OutlinedButton(
                        onClick = { onClose() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.white),
                            contentColor = colorResource(id = R.color.cancel_button_text)
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun BikeItemCart(bike: Bike, onRemoveItem: (Bike) -> Unit, onViewItem: (Bike) -> Unit) {
    val context = LocalContext.current
    val imageId = context.resources.getIdentifier(bike.imageRes, "drawable", context.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onViewItem(bike)
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageId != 0) {
            Image(
                painter = painterResource(id = imageId),
                contentDescription = bike.name,
                modifier = Modifier.size(100.dp)
            )
        } else {
            // Placeholder image or error handling
            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                contentDescription = "Image not found",
                modifier = Modifier.size(100.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(bike.name, modifier = Modifier.padding(12.dp))
        IconButton(
            onClick = { onRemoveItem(bike) }, modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(60.dp)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = "Remove"
            )
        }
    }
}



@Composable
fun VerifyOrderDialog(
    name: String,
    address: String,
    paymentMethod: String,
    cartItems: List<Bike>,
    onDismiss: () -> Unit,
    onOrderVerified: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Verify Your Order", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Name: $name")
                Text("Address: $address")
                Text("Payment Method: $paymentMethod")
                Spacer(modifier = Modifier.height(16.dp))

                Text("Ordered Bikes:", fontWeight = FontWeight.Bold)
                LazyColumn {
                    items(cartItems) { bike ->
                        Text("- ${bike.name}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Calculate total cost and item count
                val numOfItems = cartItems.count()
                val totalPrice = cartItems.sumOf { it.pricePerHour }

                // Display total cost and item count
                Text("Total: $totalPrice$/hour", fontWeight = FontWeight.Bold)
                Text("Items: $numOfItems")
                Spacer(modifier = Modifier.height(16.dp))



                Button(
                    onClick = {
                        onOrderVerified()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.cancel_button_text),
                        contentColor = colorResource(id = R.color.black))
                ) {
                    Text("Verify Order")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    cartItems: List<Bike>,
    onDismiss: () -> Unit,
    onOrderVerified: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Card") }
    var isPaymentDropdownExpanded by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isCardPayment by remember { mutableStateOf(true) }

    val paymentMethods = listOf("Card", "Cash on Pickup")

    Dialog(onDismissRequest = onDismiss) {
        Card() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Checkout", modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isPaymentDropdownExpanded,
                    onExpandedChange = { isPaymentDropdownExpanded = !isPaymentDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isPaymentDropdownExpanded,
                        onDismissRequest = { isPaymentDropdownExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    isPaymentDropdownExpanded = false
                                    isCardPayment = method == "Card"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isCardPayment) {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = {
                            if (it.length <= 16 && it.all { char -> char.isDigit() }) {
                                cardNumber = it
                            }
                        },
                        label = { Text("Card Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date (MM/YY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                                cvv = it
                            }
                        },
                        label = { Text("CVV") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        // Process the checkout information here
                        if (isCardPayment) {
                            Log.d(
                                "CheckoutDialog",
                                "Name: $name, Address: $address, Payment Method: $selectedPaymentMethod, Card Number: $cardNumber, Expiry Date: $expiryDate, CVV: $cvv"
                            )
                        } else {
                            Log.d(
                                "CheckoutDialog",
                                "Name: $name, Address: $address, Payment Method: $selectedPaymentMethod"
                            )
                        }
                        onOrderVerified(name, address, selectedPaymentMethod)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.cancel_button_text),
                        contentColor = colorResource(id = R.color.black)),
                    enabled = (if (isCardPayment) cardNumber.length == 16 && cvv.length == 3 && name.isNotEmpty() && address.isNotEmpty() && expiryDate.isNotEmpty() else name.isNotEmpty() && address.isNotEmpty())
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}


@Composable
fun ConfirmationDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = message, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.cancel_button_text),
                        contentColor = colorResource(id = R.color.black)
                    ),
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun SearchDialog(bikes: List<Bike>, onDismiss: () -> Unit, onBikeClick: (Bike) -> Unit) {
    var searchText by remember { mutableStateOf("") }
    val filteredBikes = remember(searchText) {
        if (searchText.isBlank()) {
            emptyList()
        } else {
            bikes.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .border(2.dp, colorResource(id = R.color.black), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Search Bikes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Enter bike name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Make the results scrollable
                Box(modifier = Modifier.heightIn(max = 300.dp)) { // Set a max height
                    if (filteredBikes.isEmpty()) {
                        Text("No bikes found.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn {
                            items(filteredBikes) { bike ->
                                BikeItemSearch(bike = bike, onClick = {
                                    Log.d("SearchDialog", "Bike clicked: $bike")
                                    onBikeClick(bike)
                                })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.white),
                        contentColor = colorResource(id = R.color.outlined_button_text)
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun BikeItemSearch(bike: Bike, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageId = context.resources.getIdentifier(bike.imageRes, "drawable", context.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageId != 0) {
            Image(
                painter = painterResource(id = imageId),
                contentDescription = bike.name,
                modifier = Modifier.size(50.dp)
            )
        } else {
            // Placeholder image or error handling
            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                contentDescription = "Image not found",
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(bike.name)
    }
}
