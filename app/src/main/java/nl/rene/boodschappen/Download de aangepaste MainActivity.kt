package nl.rene.boodschappen

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val Green = Color(0xFF087A43)
private val Pale = Color(0xFFEAF6EE)
private val Canvas = Color(0xFFF7F7F5)
private val Ink = Color(0xFF101820)
private val LineGreen = Color(0xFF14A05A)

// Versie 7: losse menu's, een keuze per dag en duidelijk verslepen.
data class Product(val name: String, val uses: Int = 0, val quantity: String = "")
data class Grocery(val id: Long, val name: String, val quantity: String = "", val checked: Boolean = false)
data class GroceryList(val id: Long, val name: String, val items: List<Grocery> = emptyList())
data class Ingredient(val name: String, val quantity: String = "")
data class Meal(val id: Long, val name: String, val ingredients: List<Ingredient> = emptyList())
data class DayChoice(val day: String, val mealId: Long? = null)

private val dayNames = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag", "Zaterdag", "Zondag")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { GroceryApp(this) } }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White, onBackground = Ink),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}

@Composable
fun GroceryApp(context: Context) {
    val store = remember { AppStore(context) }
    var tab by remember { mutableIntStateOf(0) }
    var listId by remember { mutableLongStateOf(store.lists.first().id) }
    Scaffold(containerColor = Canvas, bottomBar = { BottomBar(tab) { tab = it } }) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> ShoppingScreen(store, listId) { listId = it }
                1 -> MenuScreen(store, listId)
                else -> ListsScreen(store, listId, { listId = it }, { tab = it })
            }
        }
    }
}

@Composable
fun PageTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
}

// -------------------- BOODSCHAPPEN --------------------

@Composable
fun ShoppingScreen(store: AppStore, listId: Long, select: (Long) -> Unit) {
    val list = store.lists.firstOrNull { it.id == listId } ?: store.lists.first()
    var chooseList by remember { mutableStateOf(false) }
    var addProduct by remember { mutableStateOf(false) }
    var newList by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }
    val open = list.items.filterNot { it.checked }
    val done = list.items.filter { it.checked }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { PageTitle("Boodschappen") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp), color = Color.White,
                        modifier = Modifier.fillMaxWidth().clickable { chooseList = true }
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(list.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                    DropdownMenu(chooseList, { chooseList = false }) {
                        store.lists.forEach { l ->
                            DropdownMenuItem({ Text(l.name) }, { select(l.id); chooseList = false })
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton({ newList = true }) { Icon(Icons.Default.Add, "Nieuwe lijst") }
            }
        }
        item {
            OutlinedButton(
                { addProduct = true },
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                Text("Product toevoegen…", modifier = Modifier.weight(1f))
            }
        }
        item { CompactHeader("Lijstje", open.size, false) {} }
        item {
            ReorderableGroceryList(
                rows = open,
                onToggle = store::toggle,
                onRemove = store::remove,
                onQuantity = store::setQuantity,
                onMoveOne = { id, direction -> store.moveOpenOneStep(list.id, id, direction) }
            )
        }
        if (done.isNotEmpty()) {
            item { CompactHeader("Gepakt", done.size, !showDone) { showDone = !showDone } }
            if (showDone) item { StaticGroceryList(done, store::toggle, store::remove, store::setQuantity) }
            if (showDone) item { TextButton({ store.clearDone(list.id) }, Modifier.fillMaxWidth()) { Text("Afgevinkte verwijderen") } }
        }
    }
    if (addProduct) AddProductDialog(store.products, { addProduct = false }) { name, qty ->
        store.addGrocery(list.id, name, qty); addProduct = false
    }
    if (newList) TextDialog("Nieuwe boodschappenlijst", "Naam", { newList = false }) {
        select(store.addList(it)); newList = false
    }
}

@Composable
fun CompactHeader(text: String, count: Int, collapsed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$text ($count)", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        if (text == "Gepakt") Icon(if (collapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown, null)
    }
}

@Composable
fun ReorderableGroceryList(
    rows: List<Grocery>,
    onToggle: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onQuantity: (Long, String) -> Unit,
    onMoveOne: (Long, Int) -> Unit
) {
    if (rows.isEmpty()) {
        Text("Nog geen producten", color = Color.Gray, modifier = Modifier.padding(20.dp)); return
    }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 34.dp.toPx() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var direction by remember { mutableIntStateOf(0) }

    Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column {
            rows.forEachIndexed { index, grocery ->
                key(grocery.id) {
                    ReorderableProductRow(
                        grocery = grocery,
                        isDragging = draggingId == grocery.id,
                        markerDirection = if (draggingId == grocery.id) direction else 0,
                        canMoveUp = index > 0,
                        canMoveDown = index < rows.lastIndex,
                        thresholdPx = thresholdPx,
                        onDragStart = { draggingId = grocery.id },
                        onDirection = { direction = it },
                        onMove = { step -> onMoveOne(grocery.id, step) },
                        onDragEnd = { draggingId = null; direction = 0 },
                        onToggle = { onToggle(grocery.id) },
                        onRemove = { onRemove(grocery.id) },
                        onQuantity = { onQuantity(grocery.id, it) }
                    )
                }
                if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(start = 50.dp), color = Color(0xFFE9E9E9))
            }
        }
    }
    Text(
        "Houd de greep vast en sleep. De groene lijn toont de exacte nieuwe plek.",
        color = Color.Gray, fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun ReorderableProductRow(
    grocery: Grocery,
    isDragging: Boolean,
    markerDirection: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    thresholdPx: Float,
    onDragStart: () -> Unit,
    onDirection: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onQuantity: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var dragY by remember(grocery.id) { mutableFloatStateOf(0f) }
    var editQty by remember { mutableStateOf(false) }
    var qtyText by remember(grocery.id, grocery.quantity) { mutableStateOf(grocery.quantity) }

    Column(Modifier.fillMaxWidth().background(if (isDragging) Pale else Color.Transparent)) {
        if (isDragging && markerDirection < 0) DropMarker("Hier plaatsen")
        Row(
            Modifier.fillMaxWidth()
                .graphicsLayer { alpha = if (isDragging) .82f else 1f }
                .heightIn(min = 56.dp).padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onToggle, Modifier.size(44.dp)) {
                Icon(if (grocery.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = Green)
            }
            Text(
                grocery.name,
                modifier = Modifier.weight(1f).clickable { onToggle() },
                maxLines = 1, fontSize = 16.sp,
                textDecoration = if (grocery.checked) TextDecoration.LineThrough else null
            )
            Text(
                grocery.quantity.ifBlank { "Hoeveel" },
                color = if (grocery.quantity.isBlank()) Color.Gray else Ink,
                fontSize = 14.sp, maxLines = 1,
                modifier = Modifier.clickable { editQty = true }.padding(horizontal = 8.dp, vertical = 14.dp)
            )
            Icon(
                Icons.Default.DragHandle, "Vasthouden en slepen",
                tint = if (isDragging) Green else Color.Gray,
                modifier = Modifier.size(44.dp).padding(10.dp).pointerInput(grocery.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragY = 0f; onDragStart()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = { dragY = 0f; onDragEnd() },
                        onDragEnd = { dragY = 0f; onDragEnd() },
                        onDrag = { change, amount ->
                            change.consume(); dragY += amount.y
                            val dir = when {
                                dragY <= -thresholdPx -> -1
                                dragY >= thresholdPx -> 1
                                dragY < 0 -> -1
                                dragY > 0 -> 1
                                else -> 0
                            }
                            onDirection(dir)
                            if (dragY <= -thresholdPx) {
                                onMove(-1); dragY += thresholdPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragY >= thresholdPx) {
                                onMove(1); dragY -= thresholdPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            )
            IconButton(onRemove, Modifier.size(40.dp)) { Icon(Icons.Default.Close, "Verwijderen", tint = Color.Gray) }
        }
        if (isDragging && markerDirection > 0) DropMarker("Hier plaatsen")
    }

    if (editQty) AlertDialog(
        onDismissRequest = { editQty = false }, title = { Text(grocery.name) },
        text = { OutlinedTextField(qtyText, { qtyText = it }, label = { Text("Hoeveelheid") }, singleLine = true) },
        confirmButton = { Button({ onQuantity(qtyText); editQty = false }) { Text("Opslaan") } },
        dismissButton = { TextButton({ editQty = false }) { Text("Annuleren") } }
    )
}

@Composable
private fun DropMarker(text: String) {
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), thickness = 3.dp, color = LineGreen)
        Text(text, color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = 3.dp, color = LineGreen)
    }
}

@Composable
fun StaticGroceryList(rows: List<Grocery>, toggle: (Long) -> Unit, remove: (Long) -> Unit, setQty: (Long, String) -> Unit) {
    Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column {
            rows.forEachIndexed { index, g ->
                SimpleProductRow(g, { toggle(g.id) }, { remove(g.id) }, { setQty(g.id, it) })
                if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(start = 50.dp), color = Color(0xFFE9E9E9))
            }
        }
    }
}

@Composable
fun SimpleProductRow(g: Grocery, toggle: () -> Unit, remove: () -> Unit, setQty: (String) -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var quantity by remember(g.id, g.quantity) { mutableStateOf(g.quantity) }
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(toggle, Modifier.size(44.dp)) { Icon(Icons.Default.CheckCircle, null, tint = Green) }
        Text(g.name, Modifier.weight(1f), textDecoration = TextDecoration.LineThrough)
        Text(g.quantity, modifier = Modifier.clickable { edit = true }.padding(10.dp), color = Color.Gray)
        IconButton(remove) { Icon(Icons.Default.Close, "Verwijderen", tint = Color.Gray) }
    }
    if (edit) AlertDialog(
        onDismissRequest = { edit = false }, title = { Text(g.name) },
        text = { OutlinedTextField(quantity, { quantity = it }, label = { Text("Hoeveelheid") }) },
        confirmButton = { Button({ setQty(quantity); edit = false }) { Text("Opslaan") } },
        dismissButton = { TextButton({ edit = false }) { Text("Annuleren") } }
    )
}

@Composable
fun AddProductDialog(products: List<Product>, dismiss: () -> Unit, add: (String, String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    val matches = products.filter { search.isBlank() || it.name.contains(search, true) }
        .sortedWith(compareByDescending<Product> { it.uses }.thenBy { it.name })
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("Product toevoegen") },
        text = {
            Column {
                OutlinedTextField(search, { search = it }, label = { Text("Zoek of typ product") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(qty, { qty = it }, label = { Text("Hoeveel, bijvoorbeeld 500 gr") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Vaak gebruikt / producten", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(matches.take(15), key = { it.name }) { p ->
                        ListItem({ Text(p.name) }, trailingContent = { Text(p.quantity) }, modifier = Modifier.clickable { add(p.name, qty.ifBlank { p.quantity }) })
                    }
                }
            }
        },
        confirmButton = { Button({ add(search.trim(), qty.trim()) }, enabled = search.isNotBlank()) { Text("Toevoegen") } },
        dismissButton = { TextButton(dismiss) { Text("Annuleren") } }
    )
}

// -------------------- MENU PER DAG --------------------

@Composable
fun MenuScreen(store: AppStore, listId: Long) {
    var chooseDay by remember { mutableStateOf<String?>(null) }
    var showMeals by remember { mutableStateOf(false) }
    val groceryList = store.lists.firstOrNull { it.id == listId } ?: store.lists.first()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { PageTitle("Menu") }
        item { Text("Kies voor iedere dag een menu uit Mijn menu’s.", color = Color.Gray, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) }
        items(store.dayChoices, key = { it.day }) { choice ->
            val meal = store.meals.firstOrNull { it.id == choice.mealId }
            Surface(
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clickable { chooseDay = choice.day }
            ) {
                Row(Modifier.heightIn(min = 62.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(choice.day.take(2), fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                    Column(Modifier.weight(1f)) {
                        Text(meal?.name ?: "+ Menu kiezen", color = if (meal == null) Green else Ink, fontWeight = if (meal == null) FontWeight.Bold else FontWeight.Normal)
                        if (meal != null && meal.ingredients.isNotEmpty()) Text("${meal.ingredients.size} producten", color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color(0xFFE9E9E9))
        }
        item {
            TextButton({ showMeals = !showMeals }, Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Icon(if (showMeals) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, null)
                Text("Mijn menu’s (${store.meals.size})", fontWeight = FontWeight.Bold)
            }
        }
        if (showMeals) items(store.meals, key = { it.id }) { meal ->
            ListItem(
                headlineContent = { Text(meal.name) },
                supportingContent = { Text(meal.ingredients.joinToString { it.name }.ifBlank { "Nog geen producten" }, maxLines = 1) },
                colors = ListItemDefaults.colors(containerColor = Color.White)
            )
        }
    }

    chooseDay?.let { day ->
        MealChoiceDialog(
            day = day, meals = store.meals,
            dismiss = { chooseDay = null },
            clear = { store.chooseMeal(day, null); chooseDay = null },
            choose = { meal, addProducts ->
                store.chooseMeal(day, meal.id)
                if (addProducts) meal.ingredients.forEach { store.addGrocery(groceryList.id, it.name, it.quantity) }
                chooseDay = null
            }
        )
    }
}

@Composable
fun MealChoiceDialog(day: String, meals: List<Meal>, dismiss: () -> Unit, clear: () -> Unit, choose: (Meal, Boolean) -> Unit) {
    var selected by remember { mutableStateOf<Meal?>(null) }
    var addProducts by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("$day: kies een menu") },
        text = {
            Column {
                if (meals.isEmpty()) Text("Maak eerst een menu aan via Lijsten.")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(meals, key = { it.id }) { meal ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = meal }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected == meal, { selected = meal })
                            Column(Modifier.weight(1f)) {
                                Text(meal.name, fontWeight = FontWeight.Bold)
                                Text("${meal.ingredients.size} producten", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(addProducts, { addProducts = it })
                    Text("Producten toevoegen aan boodschappenlijst")
                }
            }
        },
        confirmButton = { Button({ selected?.let { choose(it, addProducts) } }, enabled = selected != null) { Text("Kiezen") } },
        dismissButton = { Row { TextButton(clear) { Text("Leegmaken") }; TextButton(dismiss) { Text("Annuleren") } } }
    )
}

// -------------------- LIJSTEN BEHEREN --------------------

@Composable
fun ListsScreen(store: AppStore, selectedId: Long, select: (Long) -> Unit, navigate: (Int) -> Unit) {
    var newList by remember { mutableStateOf(false) }
    var editList by remember { mutableStateOf<GroceryList?>(null) }
    var newProduct by remember { mutableStateOf(false) }
    var editProduct by remember { mutableStateOf<Product?>(null) }
    var newMeal by remember { mutableStateOf(false) }
    var editMeal by remember { mutableStateOf<Meal?>(null) }
    var alphabetical by remember { mutableStateOf(true) }
    val shown = if (alphabetical) store.products.sortedBy { it.name.lowercase(Locale("nl", "NL")) } else store.products.sortedByDescending { it.uses }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { PageTitle("Lijsten") }
        item { ManageTitle("Boodschappenlijsten") }
        items(store.lists, key = { it.id }) { list ->
            ManageRow(list.name, "${list.items.count { !it.checked }}", { editList = list }, if (store.lists.size > 1) {{ store.deleteList(list.id) }} else null)
        }
        item { AddRow("Nieuwe boodschappenlijst") { newList = true } }

        item { ManageTitle("Menu’s") }
        items(store.meals, key = { it.id }) { meal ->
            ManageRow(meal.name, "${meal.ingredients.size} producten", { editMeal = meal }, { store.deleteMeal(meal.id) })
        }
        item { AddRow("Nieuw menu") { newMeal = true } }

        item {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, top = 20.dp, end = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Producten", fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.weight(1f))
                FilterChip(alphabetical, { alphabetical = true }, { Text("A–Z") })
                Spacer(Modifier.width(6.dp))
                FilterChip(!alphabetical, { alphabetical = false }, { Text("Vaak") })
            }
        }
        items(shown, key = { it.name }) { p -> ManageRow(p.name, p.quantity, { editProduct = p }, { store.deleteProduct(p.name) }) }
        item { AddRow("Nieuw product") { newProduct = true } }
    }

    if (newList) TextDialog("Nieuwe boodschappenlijst", "Naam", { newList = false }) { select(store.addList(it)); newList = false; navigate(0) }
    editList?.let { list -> RenameDialog("Boodschappenlijst wijzigen", list.name, { editList = null }) { store.renameList(list.id, it); editList = null } }
    if (newProduct) ProductDialog(null, { newProduct = false }) { old, name, qty -> store.saveProduct(old, name, qty); newProduct = false }
    editProduct?.let { p -> ProductDialog(p, { editProduct = null }) { old, name, qty -> store.saveProduct(old, name, qty); editProduct = null } }
    if (newMeal) MealEditorDialog(null, store.products, { newMeal = false }) { store.saveMeal(null, it); newMeal = false }
    editMeal?.let { meal -> MealEditorDialog(meal, store.products, { editMeal = null }) { store.saveMeal(meal.id, it); editMeal = null } }
}

@Composable fun ManageTitle(text: String) { Text(text, fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp)) }

@Composable
fun ManageRow(name: String, detail: String, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    ListItem(
        headlineContent = { Text(name) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detail, color = Color.Gray); Icon(Icons.Default.Edit, "Bewerken", tint = Green)
                if (onDelete != null) IconButton(onDelete) { Icon(Icons.Default.DeleteOutline, "Verwijderen") }
            }
        },
        modifier = Modifier.clickable { onClick() }, colors = ListItemDefaults.colors(containerColor = Color.White)
    )
}

@Composable
fun AddRow(text: String, onClick: () -> Unit) {
    ListItem({ Text(text, color = Green, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(Icons.Default.Add, null, tint = Green) }, modifier = Modifier.clickable { onClick() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
}

@Composable
fun MealEditorDialog(existing: Meal?, products: List<Product>, dismiss: () -> Unit, save: (Meal) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    val ingredients = remember(existing?.id) { mutableStateListOf<Ingredient>().apply { addAll(existing?.ingredients ?: emptyList()) } }
    var addIngredient by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text(if (existing == null) "Nieuw menu" else "Menu bewerken") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Naam van het menu") }, modifier = Modifier.fillMaxWidth())
                Text("Producten", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(ingredients, key = { it.name }) { ingredient ->
                        ListItem(
                            headlineContent = { Text(ingredient.name) }, supportingContent = { Text(ingredient.quantity) },
                            trailingContent = { IconButton({ ingredients.remove(ingredient) }) { Icon(Icons.Default.Close, "Verwijderen") } }
                        )
                    }
                }
                OutlinedButton({ addIngredient = true }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Product toevoegen") }
            }
        },
        confirmButton = {
            Button({ save(Meal(existing?.id ?: System.currentTimeMillis(), name.trim(), ingredients.toList())) }, enabled = name.isNotBlank()) { Text("Opslaan") }
        },
        dismissButton = { TextButton(dismiss) { Text("Annuleren") } }
    )
    if (addIngredient) IngredientDialog(products, { addIngredient = false }) { ingredient ->
        val i = ingredients.indexOfFirst { it.name.equals(ingredient.name, true) }
        if (i >= 0) ingredients[i] = ingredient else ingredients.add(ingredient)
        addIngredient = false
    }
}

@Composable
fun IngredientDialog(products: List<Product>, dismiss: () -> Unit, save: (Ingredient) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("Product aan menu toevoegen") },
        text = {
            Column {
                Box {
                    OutlinedTextField(name, { name = it }, label = { Text("Product") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton({ menu = true }) { Icon(Icons.Default.KeyboardArrowDown, null) } })
                    DropdownMenu(menu, { menu = false }) {
                        products.sortedBy { it.name }.forEach { p -> DropdownMenuItem({ Text(p.name) }, { name = p.name; quantity = p.quantity; menu = false }) }
                    }
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Hoeveelheid") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button({ save(Ingredient(name.trim(), quantity.trim())) }, enabled = name.isNotBlank()) { Text("Toevoegen") } },
        dismissButton = { TextButton(dismiss) { Text("Annuleren") } }
    )
}

@Composable
fun TextDialog(title: String, label: String, dismiss: () -> Unit, save: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(text, { text = it }, label = { Text(label) }) }, confirmButton = { Button({ save(text.trim()) }, enabled = text.isNotBlank()) { Text("Opslaan") } }, dismissButton = { TextButton(dismiss) { Text("Annuleren") } })
}

@Composable
fun RenameDialog(title: String, initial: String, dismiss: () -> Unit, save: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(text, { text = it }, label = { Text("Naam") }) }, confirmButton = { Button({ save(text.trim()) }, enabled = text.isNotBlank()) { Text("Opslaan") } }, dismissButton = { TextButton(dismiss) { Text("Annuleren") } })
}

@Composable
fun ProductDialog(existing: Product?, dismiss: () -> Unit, save: (String?, String, String) -> Unit) {
    var name by remember(existing?.name) { mutableStateOf(existing?.name ?: "") }
    var quantity by remember(existing?.name) { mutableStateOf(existing?.quantity ?: "") }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text(if (existing == null) "Nieuw product" else "Product bewerken") },
        text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Product") }); OutlinedTextField(quantity, { quantity = it }, label = { Text("Standaardhoeveelheid") }) } },
        confirmButton = { Button({ save(existing?.name, name.trim(), quantity.trim()) }, enabled = name.isNotBlank()) { Text("Opslaan") } },
        dismissButton = { TextButton(dismiss) { Text("Annuleren") } }
    )
}

@Composable
fun BottomBar(tab: Int, change: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        listOf(Triple("Boodschappen", Icons.Default.ShoppingCart, 0), Triple("Menu", Icons.Default.Restaurant, 1), Triple("Lijsten", Icons.Default.List, 2)).forEach { (text, icon, number) ->
            NavigationBarItem(tab == number, { change(number) }, { Icon(icon, null) }, label = { Text(text) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Pale, selectedIconColor = Green, selectedTextColor = Green))
        }
    }
}

// -------------------- OPSLAG EN MIGRATIE --------------------

class AppStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("grocery_v2", Context.MODE_PRIVATE)
    var lists by mutableStateOf<List<GroceryList>>(emptyList()); private set
    var products by mutableStateOf<List<Product>>(emptyList()); private set
    var meals by mutableStateOf<List<Meal>>(emptyList()); private set
    var dayChoices by mutableStateOf(dayNames.map { DayChoice(it) }); private set

    init { load() }

    fun addList(name: String): Long { val id = System.currentTimeMillis(); lists = lists + GroceryList(id, name); save(); return id }
    fun renameList(id: Long, name: String) { lists = lists.map { if (it.id == id) it.copy(name = name) else it }; save() }
    fun deleteList(id: Long) { if (lists.size > 1) lists = lists.filterNot { it.id == id }; save() }

    fun addGrocery(listId: Long, name: String, quantity: String) {
        if (name.isBlank()) return
        val grocery = Grocery(System.nanoTime(), name.trim(), quantity.trim())
        lists = lists.map { if (it.id == listId) it.copy(items = it.items + grocery) else it }
        val old = products.firstOrNull { it.name.equals(name, true) }
        products = if (old == null) products + Product(name.trim(), 1, quantity.trim()) else products.map {
            if (it == old) it.copy(uses = it.uses + 1, quantity = quantity.ifBlank { it.quantity }) else it
        }
        save()
    }

    private fun listOfItem(id: Long) = lists.firstNotNullOfOrNull { list -> list.items.firstOrNull { it.id == id }?.let { list.id } }
    fun toggle(id: Long) { val lid = listOfItem(id) ?: return; lists = lists.map { if (it.id == lid) it.copy(items = it.items.map { g -> if (g.id == id) g.copy(checked = !g.checked) else g }) else it }; save() }
    fun remove(id: Long) { val lid = listOfItem(id) ?: return; lists = lists.map { if (it.id == lid) it.copy(items = it.items.filterNot { g -> g.id == id }) else it }; save() }
    fun setQuantity(id: Long, quantity: String) { val lid = listOfItem(id) ?: return; lists = lists.map { if (it.id == lid) it.copy(items = it.items.map { g -> if (g.id == id) g.copy(quantity = quantity) else g }) else it }; save() }
    fun clearDone(listId: Long) { lists = lists.map { if (it.id == listId) it.copy(items = it.items.filterNot { g -> g.checked }) else it }; save() }

    fun moveOpenOneStep(listId: Long, id: Long, direction: Int) {
        if (direction == 0) return
        lists = lists.map { list ->
            if (list.id != listId) list else {
                val open = list.items.filterNot { it.checked }.toMutableList()
                val done = list.items.filter { it.checked }
                val from = open.indexOfFirst { it.id == id }
                val target = (from + direction).coerceIn(0, open.lastIndex)
                if (from >= 0 && from != target) {
                    val moving = open.removeAt(from); open.add(target, moving)
                }
                list.copy(items = open + done)
            }
        }
        save()
    }

    fun saveProduct(oldName: String?, name: String, quantity: String) {
        if (name.isBlank()) return
        val old = oldName?.let { n -> products.firstOrNull { it.name == n } }
        products = if (old == null) {
            if (products.none { it.name.equals(name, true) }) products + Product(name, 0, quantity) else products
        } else products.map { if (it == old) it.copy(name = name, quantity = quantity) else it }
        if (oldName != null && oldName != name) {
            lists = lists.map { l -> l.copy(items = l.items.map { if (it.name == oldName) it.copy(name = name) else it }) }
            meals = meals.map { m -> m.copy(ingredients = m.ingredients.map { if (it.name == oldName) it.copy(name = name) else it }) }
        }
        save()
    }
    fun deleteProduct(name: String) { products = products.filterNot { it.name == name }; save() }

    fun saveMeal(id: Long?, meal: Meal) {
        meals = if (id == null) meals + meal else meals.map { if (it.id == id) meal.copy(id = id) else it }
        save()
    }
    fun deleteMeal(id: Long) { meals = meals.filterNot { it.id == id }; dayChoices = dayChoices.map { if (it.mealId == id) it.copy(mealId = null) else it }; save() }
    fun chooseMeal(day: String, mealId: Long?) { dayChoices = dayChoices.map { if (it.day == day) it.copy(mealId = mealId) else it }; save() }

    private fun save() {
        val root = JSONObject()
        root.put("lists", JSONArray().apply { lists.forEach { l -> put(JSONObject().put("id", l.id).put("name", l.name).put("items", JSONArray().apply { l.items.forEach { g -> put(JSONObject().put("id", g.id).put("name", g.name).put("quantity", g.quantity).put("checked", g.checked)) } })) } })
        root.put("products", JSONArray().apply { products.forEach { put(JSONObject().put("name", it.name).put("uses", it.uses).put("quantity", it.quantity)) } })
        root.put("meals", JSONArray().apply { meals.forEach { m -> put(JSONObject().put("id", m.id).put("name", m.name).put("ingredients", JSONArray().apply { m.ingredients.forEach { i -> put(JSONObject().put("name", i.name).put("quantity", i.quantity)) } })) } })
        root.put("days", JSONArray().apply { dayChoices.forEach { put(JSONObject().put("day", it.day).put("mealId", it.mealId ?: JSONObject.NULL)) } })
        prefs.edit().putString("data_v7", root.toString()).apply()
    }

    private fun load() {
        try {
            val raw = prefs.getString("data_v7", null)
            if (raw != null) loadV7(JSONObject(raw)) else migrateV6()
        } catch (_: Exception) { }
        if (lists.isEmpty()) lists = listOf(GroceryList(1, "Weekboodschappen", listOf(Grocery(11, "Melk", "1 pak"), Grocery(12, "Brood", "1 brood"))))
        if (products.isEmpty()) products = listOf(Product("Melk", 8, "1 pak"), Product("Brood", 7, "1 brood"), Product("Gehakt", 6, "500 gr"), Product("Bananen", 5, "6 stuks"))
        if (dayChoices.size != 7) dayChoices = dayNames.map { DayChoice(it) }
        save()
    }

    private fun loadV7(root: JSONObject) {
        lists = root.optJSONArray("lists").toGroceryLists()
        products = root.optJSONArray("products").toProducts()
        val mealArray = root.optJSONArray("meals") ?: JSONArray()
        meals = (0 until mealArray.length()).map { i ->
            val o = mealArray.getJSONObject(i); val a = o.optJSONArray("ingredients") ?: JSONArray()
            Meal(o.getLong("id"), o.getString("name"), (0 until a.length()).map { j -> val x = a.getJSONObject(j); Ingredient(x.getString("name"), x.optString("quantity")) })
        }
        val days = root.optJSONArray("days") ?: JSONArray()
        dayChoices = dayNames.map { day ->
            val found = (0 until days.length()).map { days.getJSONObject(it) }.firstOrNull { it.optString("day") == day }
            DayChoice(day, if (found == null || found.isNull("mealId")) null else found.optLong("mealId"))
        }
    }

    private fun migrateV6() {
        val raw = prefs.getString("data_v6", null) ?: return
        val root = JSONObject(raw)
        lists = root.optJSONArray("lists").toGroceryLists()
        products = root.optJSONArray("products").toProducts()
        val oldMenus = root.optJSONArray("weekMenus") ?: JSONArray()
        val foundMeals = linkedMapOf<String, Meal>()
        var firstWeekDays: JSONArray? = null
        if (oldMenus.length() > 0) firstWeekDays = oldMenus.getJSONObject(0).optJSONArray("days")
        for (i in 0 until oldMenus.length()) {
            val days = oldMenus.getJSONObject(i).optJSONArray("days") ?: continue
            for (j in 0 until days.length()) {
                val d = days.getJSONObject(j); val mealName = d.optString("meal").trim()
                if (mealName.isNotBlank() && !foundMeals.containsKey(mealName.lowercase())) foundMeals[mealName.lowercase()] = Meal(System.nanoTime() + i * 100 + j, mealName)
            }
        }
        meals = foundMeals.values.toList()
        dayChoices = dayNames.map { day ->
            var mealName = ""
            if (firstWeekDays != null) {
                for (j in 0 until firstWeekDays.length()) { val d = firstWeekDays.getJSONObject(j); if (d.optString("day") == day) mealName = d.optString("meal") }
            }
            DayChoice(day, meals.firstOrNull { it.name.equals(mealName, true) }?.id)
        }
    }

    private fun JSONArray?.toGroceryLists(): List<GroceryList> {
        val a = this ?: return emptyList()
        return (0 until a.length()).map { i -> val o = a.getJSONObject(i); val items = o.optJSONArray("items") ?: JSONArray(); GroceryList(o.getLong("id"), o.getString("name"), (0 until items.length()).map { j -> val g = items.getJSONObject(j); Grocery(g.getLong("id"), g.getString("name"), g.optString("quantity"), g.optBoolean("checked")) }) }
    }
    private fun JSONArray?.toProducts(): List<Product> {
        val a = this ?: return emptyList()
        return (0 until a.length()).map { i -> val o = a.getJSONObject(i); Product(o.getString("name"), o.optInt("uses"), o.optString("quantity")) }
    }
}
