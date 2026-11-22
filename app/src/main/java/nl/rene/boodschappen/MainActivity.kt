package nl.rene.boodschappen

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private val Green = Color(0xFF087A43)
private val PaleGreen = Color(0xFFEAF6EE)
private val Canvas = Color(0xFFF7F7F5)
private val Ink = Color(0xFF101820)

data class Product(var name:String, var uses:Int=0, var quantity:String="")
data class Grocery(var id:Long, var name:String, var quantity:String="", var checked:Boolean=false)
data class GroceryList(var id:Long, var name:String, val items:MutableList<Grocery> = mutableListOf())
data class MenuDay(var day:String, var meal:String="", var ingredients:String="")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { GroceryApplication(this) } }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            secondary = Green,
            background = Canvas,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp
            )
        ),
        content = content
    )
}

@Composable fun GroceryApplication(context: Context) {
    val store = remember { AppStore(context) }
    var stateTick by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var selectedList by remember { mutableLongStateOf(store.lists.first().id) }
    fun refresh(){ store.save(); stateTick++ }
    stateTick
    Scaffold(
        containerColor=Canvas,
        bottomBar={ AppleBottomBar(tab){tab=it} }
    ){ pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when(tab){
                0 -> ShoppingPage(store, selectedList, {selectedList=it}, ::refresh)
                1 -> WeekMenuPage(store, selectedList, ::refresh)
                else -> ListsPage(store, selectedList, {selectedList=it}, ::refresh)
            }
        }
    }
}

@Composable fun PageHeader(title:String) {
    Column(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=12.dp)) {
        Text(SimpleDateFormat("EEEE  •  d MMMM", Locale("nl","NL")).format(Date()).uppercase(), color=Color.Gray, fontSize=12.sp, letterSpacing=2.sp)
        Text(title, style=MaterialTheme.typography.headlineLarge)
    }
}

@Composable fun ShoppingPage(store:AppStore, selectedId:Long, select:(Long)->Unit, refresh:()->Unit) {
    val list = store.lists.firstOrNull{it.id==selectedId} ?: store.lists.first()
    var search by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var listMenu by remember { mutableStateOf(false) }
    var newListDialog by remember { mutableStateOf(false) }
    val suggestions = store.products.filter { search.isBlank() || it.name.contains(search,true) }.sortedByDescending{it.uses}.take(6)
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp)) {
        item { PageHeader("Boodschappen") }
        item {
            Row(Modifier.padding(horizontal=20.dp), verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Surface(shape=RoundedCornerShape(20.dp), color=Color.White, shadowElevation=2.dp, modifier=Modifier.fillMaxWidth().clickable{listMenu=true}) {
                        Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.List, null, tint=Ink); Spacer(Modifier.width(12.dp)); Text(list.name, fontWeight=FontWeight.Bold, fontSize=19.sp, modifier=Modifier.weight(1f)); Icon(Icons.Default.KeyboardArrowDown,null)
                        }
                    }
                    DropdownMenu(listMenu,{listMenu=false}) { store.lists.forEach { l -> DropdownMenuItem({Text(l.name)}, {select(l.id);listMenu=false}) } }
                }
                Spacer(Modifier.width(10.dp)); FilledIconButton(onClick={newListDialog=true}, colors=IconButtonDefaults.filledIconButtonColors(containerColor=PaleGreen, contentColor=Green), modifier=Modifier.size(58.dp)){Icon(Icons.Default.Add,"Nieuwe lijst")}
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                OutlinedTextField(search,{search=it}, modifier=Modifier.fillMaxWidth(), singleLine=true, shape=RoundedCornerShape(24.dp), leadingIcon={Icon(Icons.Default.Search,null)}, label={Text("Zoek of voeg product toe")}, trailingIcon={FilledIconButton(onClick={ if(search.isNotBlank()){store.addGrocery(list,search,quantity);search="";quantity="";refresh()} }){Icon(Icons.Default.Add,"Toevoegen")}})
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(quantity,{quantity=it},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(18.dp),label={Text("Hoeveelheid (optioneel), bijv. 2 pakken")})
                if(suggestions.isNotEmpty()) { Text("Vaak gebruikt", fontWeight=FontWeight.Bold, fontSize=21.sp, modifier=Modifier.padding(top=20.dp,bottom=8.dp)); suggestions.chunked(3).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach { p -> SuggestionChip(onClick={store.addGrocery(list,p.name,p.quantity);p.uses++;refresh()},label={Text("+ ${p.name}")}, colors=SuggestionChipDefaults.suggestionChipColors(containerColor=PaleGreen))}} } }
            }
        }
        val open=list.items.filter{!it.checked}; val done=list.items.filter{it.checked}
        item { SectionTitle("Nog te pakken",open.size) }
        item { GroceryCard(open, false, {g->g.checked=true;refresh()},{g->list.items.remove(g);refresh()}) }
        if(done.isNotEmpty()) {
            item { SectionTitle("Gepakt",done.size) }
            item { GroceryCard(done,true,{g->g.checked=false;refresh()},{g->list.items.remove(g);refresh()}) }
            item { TextButton(onClick={list.items.removeAll{it.checked};refresh()}, modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth().background(PaleGreen,RoundedCornerShape(22.dp)).padding(7.dp)){Icon(Icons.Default.Delete,null);Spacer(Modifier.width(8.dp));Text("Afgevinkte verwijderen",color=Green,fontWeight=FontWeight.Bold)} }
        }
    }
    if(newListDialog) NameDialog("Nieuwe boodschappenlijst","Naam van de lijst",{newListDialog=false}){name-> val l=store.addList(name);select(l.id);refresh();newListDialog=false}
}

@Composable fun SectionTitle(text:String,count:Int){Row(Modifier.padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text(text,fontWeight=FontWeight.Bold,fontSize=23.sp);Spacer(Modifier.width(10.dp));Surface(color=Color(0xFFE8E8E8),shape=CircleShape){Text("$count",Modifier.padding(horizontal=11.dp,vertical=5.dp),color=Color.DarkGray)}}}

@Composable fun GroceryCard(rows:List<Grocery>, checked:Boolean, toggle:(Grocery)->Unit, delete:(Grocery)->Unit) {
    if(rows.isEmpty()) { Text("Nog geen producten.",color=Color.Gray,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp));return }
    Surface(Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=if(checked) PaleGreen else Color.White,shadowElevation=1.dp){Column{rows.forEachIndexed { i,g -> Row(Modifier.fillMaxWidth().clickable{toggle(g)}.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null,tint=Green,modifier=Modifier.size(32.dp));Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(g.name,fontSize=18.sp,fontWeight=FontWeight.Medium,textDecoration=if(checked)TextDecoration.LineThrough else null,color=if(checked)Color.Gray else Ink);if(g.quantity.isNotBlank())Text(g.quantity,color=Color.Gray)};IconButton(onClick={delete(g)}){Icon(Icons.Default.DeleteOutline,"Verwijder",tint=Color.Gray)}};if(i<rows.lastIndex)HorizontalDivider(Modifier.padding(start=62.dp),color=Color(0xFFE8E8E8))}}}
}

@Composable fun WeekMenuPage(store:AppStore, selectedListId:Long, refresh:()->Unit) {
    val list=store.lists.firstOrNull{it.id==selectedListId}?:store.lists.first()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)){
        item{PageHeader("Weekmenu")}
        item{Text("Vul per dag het gerecht en eventueel ingrediënten in. Met de knop voeg je de ingrediënten toe aan ${list.name}.",color=Color.Gray,modifier=Modifier.padding(horizontal=20.dp,vertical=4.dp))}
        items(store.menu){day->
            Surface(Modifier.padding(horizontal=20.dp,vertical=7.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=Color.White,shadowElevation=1.dp){Column(Modifier.padding(16.dp)){Text(day.day,fontWeight=FontWeight.Bold,fontSize=20.sp);OutlinedTextField(day.meal,{day.meal=it;refresh()},label={Text("Wat eten we?")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp));Spacer(Modifier.height(7.dp));OutlinedTextField(day.ingredients,{day.ingredients=it;refresh()},label={Text("Ingrediënten, gescheiden door komma's")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp));TextButton(onClick={day.ingredients.split(",").map{it.trim()}.filter{it.isNotBlank()}.forEach{store.addGrocery(list,it,"")};refresh()},enabled=day.ingredients.isNotBlank()){Icon(Icons.Default.AddShoppingCart,null);Spacer(Modifier.width(6.dp));Text("Toevoegen aan boodschappen")}}}
        }
    }
}

@Composable fun ListsPage(store:AppStore, selectedId:Long, select:(Long)->Unit, refresh:()->Unit) {
    var newList by remember{mutableStateOf(false)}; var newProduct by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)){
        item{PageHeader("Lijsten")}
        item{Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({newList=true},modifier=Modifier.weight(1f)){Icon(Icons.Default.Add,null);Text(" Nieuwe lijst")};OutlinedButton({newProduct=true},modifier=Modifier.weight(1f)){Icon(Icons.Default.Add,null);Text(" Product")}}}
        item{Text("Boodschappenlijstjes",fontWeight=FontWeight.Bold,fontSize=22.sp,modifier=Modifier.padding(20.dp))}
        items(store.lists){l->Surface(Modifier.padding(horizontal=20.dp,vertical=5.dp).fillMaxWidth().clickable{select(l.id)},shape=RoundedCornerShape(19.dp),color=if(l.id==selectedId)PaleGreen else Color.White){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.List,null,tint=Green);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(l.name,fontWeight=FontWeight.Bold);Text("${l.items.count{!it.checked}} nog te pakken",color=Color.Gray)};if(store.lists.size>1)IconButton({store.lists.remove(l);refresh()}){Icon(Icons.Default.DeleteOutline,"Verwijder")}}}}
        item{Text("Vaste producten",fontWeight=FontWeight.Bold,fontSize=22.sp,modifier=Modifier.padding(20.dp))}
        items(store.products.sortedByDescending{it.uses}){p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(if(p.uses==1)"1 keer gebruikt" else "${p.uses} keer gebruikt")},trailingContent={IconButton({store.products.remove(p);refresh()}){Icon(Icons.Default.DeleteOutline,"Verwijder")}},colors=ListItemDefaults.colors(containerColor=Color.Transparent),modifier=Modifier.padding(horizontal=12.dp))}
    }
    if(newList)NameDialog("Nieuwe boodschappenlijst","Naam",{newList=false}){store.addList(it);refresh();newList=false}
    if(newProduct)NameDialog("Nieuw vast product","Productnaam",{newProduct=false}){store.addProduct(it);refresh();newProduct=false}
}

@Composable fun AppleBottomBar(tab:Int,change:(Int)->Unit){Surface(color=Color.White.copy(alpha=.96f),shadowElevation=10.dp,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)){NavigationBar(containerColor=Color.Transparent){listOf(Triple("Boodschappen",Icons.Default.ShoppingBasket,0),Triple("Weekmenu",Icons.Default.CalendarMonth,1),Triple("Lijsten",Icons.Default.Checklist,2)).forEach{(label,icon,index)->NavigationBarItem(selected=tab==index,onClick={change(index)},icon={Icon(icon,null)},label={Text(label)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Green,selectedTextColor=Green,indicatorColor=PaleGreen))}}}}

@Composable fun NameDialog(title:String,label:String,dismiss:()->Unit,save:(String)->Unit){var text by remember{mutableStateOf("")};AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={OutlinedTextField(text,{text=it},label={Text(label)},singleLine=true)},confirmButton={Button({if(text.isNotBlank())save(text.trim())},enabled=text.isNotBlank()){Text("Opslaan")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}

class AppStore(private val context:Context){
    val lists=mutableStateListOf<GroceryList>(); val products=mutableStateListOf<Product>(); val menu=mutableStateListOf<MenuDay>()
    private val prefs=context.getSharedPreferences("grocery_v2",Context.MODE_PRIVATE)
    init{load()}
    fun addList(name:String):GroceryList { val l=GroceryList(System.currentTimeMillis(),name);lists.add(l);return l }
    fun addProduct(name:String){if(products.none{it.name.equals(name,true)})products.add(Product(name))}
    fun addGrocery(list:GroceryList,name:String,quantity:String){val clean=name.trim();if(clean.isEmpty())return;list.items.add(Grocery(System.currentTimeMillis(),clean,quantity.trim()));val p=products.firstOrNull{it.name.equals(clean,true)}?:Product(clean).also{products.add(it)};p.uses++;if(quantity.isNotBlank())p.quantity=quantity.trim()}
    fun save(){val root=JSONObject();root.put("lists",JSONArray().apply{lists.forEach{l->put(JSONObject().put("id",l.id).put("name",l.name).put("items",JSONArray().apply{l.items.forEach{g->put(JSONObject().put("id",g.id).put("name",g.name).put("quantity",g.quantity).put("checked",g.checked))}}))}});root.put("products",JSONArray().apply{products.forEach{put(JSONObject().put("name",it.name).put("uses",it.uses).put("quantity",it.quantity))}});root.put("menu",JSONArray().apply{menu.forEach{put(JSONObject().put("day",it.day).put("meal",it.meal).put("ingredients",it.ingredients))}});prefs.edit().putString("data",root.toString()).apply()}
    private fun load(){try{val raw=prefs.getString("data",null);if(raw!=null){val root=JSONObject(raw);val la=root.getJSONArray("lists");for(i in 0 until la.length()){val o=la.getJSONObject(i);val l=GroceryList(o.getLong("id"),o.getString("name"));val ia=o.getJSONArray("items");for(j in 0 until ia.length()){val g=ia.getJSONObject(j);l.items.add(Grocery(g.getLong("id"),g.getString("name"),g.optString("quantity"),g.optBoolean("checked")))};lists.add(l)};val pa=root.getJSONArray("products");for(i in 0 until pa.length()){val p=pa.getJSONObject(i);products.add(Product(p.getString("name"),p.optInt("uses"),p.optString("quantity")))};val ma=root.getJSONArray("menu");for(i in 0 until ma.length()){val m=ma.getJSONObject(i);menu.add(MenuDay(m.getString("day"),m.optString("meal"),m.optString("ingredients")))}}}catch(_:Exception){}
        if(lists.isEmpty())lists.add(GroceryList(1,"Weekboodschappen",mutableListOf(Grocery(11,"Melk","2 pakken"),Grocery(12,"Brood","1 brood"))))
        if(products.isEmpty()) listOf("Melk","Brood","Bananen","Eieren","Kaas","Yoghurt","Pasta","Tomaten").forEachIndexed{i,n->products.add(Product(n,8-i))}
        if(menu.isEmpty())listOf("Maandag","Dinsdag","Woensdag","Donderdag","Vrijdag","Zaterdag","Zondag").forEach{menu.add(MenuDay(it))}
    }
}
