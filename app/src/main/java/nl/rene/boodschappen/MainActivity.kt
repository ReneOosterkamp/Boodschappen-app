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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.graphicsLayer
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

data class Product(val name:String, val uses:Int=0, val quantity:String="")
data class Grocery(val id:Long, val name:String, val quantity:String="", val checked:Boolean=false)
data class GroceryList(val id:Long, val name:String, val items:List<Grocery> = emptyList())
data class MenuDay(val day:String, val meal:String="", val ingredients:String="")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { GroceryApplication(this) } }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary=Green, secondary=Green, background=Canvas, surface=Color.White, onPrimary=Color.White, onBackground=Ink),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontSize=38.sp,fontWeight=FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.Bold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize=17.sp)
        ),
        content=content
    )
}

@Composable
fun GroceryApplication(context:Context) {
    val store=remember{AppStore(context)}
    var tab by remember{mutableIntStateOf(0)}
    var selectedId by remember{mutableLongStateOf(store.lists.first().id)}
    Scaffold(containerColor=Canvas,bottomBar={BottomBar(tab){tab=it}}){pad->
        Box(Modifier.padding(pad).fillMaxSize()){
            when(tab){
                0->ShoppingPage(store,selectedId,{selectedId=it})
                1->WeekMenuPage(store,selectedId)
                else->ListsPage(store,selectedId,{selectedId=it})
            }
        }
    }
}

@Composable fun Header(title:String){Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp)){Text(SimpleDateFormat("EEEE  •  d MMMM",Locale("nl","NL")).format(Date()).uppercase(),color=Color.Gray,fontSize=12.sp,letterSpacing=2.sp);Text(title,style=MaterialTheme.typography.headlineLarge)}}

@Composable
fun ShoppingPage(store:AppStore,selectedId:Long,select:(Long)->Unit){
    val list=store.lists.firstOrNull{it.id==selectedId}?:store.lists.first()
    var search by remember{mutableStateOf("")};var quantity by remember{mutableStateOf("")};var listMenu by remember{mutableStateOf(false)};var picker by remember{mutableStateOf(false)};var newList by remember{mutableStateOf(false)}
    val frequent=store.products.filter{search.isBlank()||it.name.contains(search,true)}.sortedByDescending{it.uses}.take(5)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=26.dp)){
        item{Header("Boodschappen")}
        item{Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Surface(shape=RoundedCornerShape(20.dp),color=Color.White,shadowElevation=2.dp,modifier=Modifier.fillMaxWidth().clickable{listMenu=true}){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.List,null,tint=Green);Spacer(Modifier.width(12.dp));Text(list.name,fontWeight=FontWeight.Bold,fontSize=19.sp,modifier=Modifier.weight(1f));Icon(Icons.Default.KeyboardArrowDown,null)}};DropdownMenu(listMenu,{listMenu=false}){store.lists.forEach{l->DropdownMenuItem({Text(l.name)},{select(l.id);listMenu=false})}}};Spacer(Modifier.width(10.dp));FilledIconButton({newList=true},modifier=Modifier.size(58.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=PaleGreen,contentColor=Green)){Icon(Icons.Default.Add,"Nieuwe lijst")}}}
        item{Column(Modifier.padding(20.dp)){OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(22.dp),label={Text("Product")},leadingIcon={Icon(Icons.Default.Search,null)});Spacer(Modifier.height(8.dp));OutlinedTextField(quantity,{quantity=it},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(18.dp),label={Text("Hoeveel, bijvoorbeeld 500 gr of 1 pak")});Button(onClick={if(search.isNotBlank()){store.addGrocery(list.id,search,quantity);search="";quantity=""}},enabled=search.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Direct op lijstje zetten")};OutlinedButton({picker=true},modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Icon(Icons.Default.Checklist,null);Spacer(Modifier.width(7.dp));Text("Kies uit alle producten")}
            if(frequent.isNotEmpty()){Text("Vaak gebruikt",fontWeight=FontWeight.Bold,fontSize=21.sp,modifier=Modifier.padding(top=20.dp,bottom=8.dp));Column(verticalArrangement=Arrangement.spacedBy(8.dp)){frequent.forEach{p->FrequentRow(p){qty->store.addGrocery(list.id,p.name,qty)}}}}
        }}
        val open=list.items.filter{!it.checked};val done=list.items.filter{it.checked}
        item{SectionTitle("Lijstje",open.size)}
        item{GroceryCard(open,store::toggleGrocery,store::deleteGrocery,{id,q->store.updateQuantity(list.id,id,q)},{id,d->store.moveGrocery(list.id,id,d)})}
        if(done.isNotEmpty()){item{SectionTitle("Gepakt",done.size)};item{GroceryCard(done,store::toggleGrocery,store::deleteGrocery,{id,q->store.updateQuantity(list.id,id,q)},null)};item{TextButton({store.clearChecked(list.id)},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth().background(PaleGreen,RoundedCornerShape(22.dp)).padding(7.dp)){Icon(Icons.Default.Delete,null);Spacer(Modifier.width(8.dp));Text("Afgevinkte verwijderen",color=Green,fontWeight=FontWeight.Bold)}}}
    }
    if(picker)ProductPicker(store.products,{picker=false}){chosen->chosen.forEach{(p,q)->store.addGrocery(list.id,p.name,q)};picker=false}
    if(newList)NameDialog("Nieuwe boodschappenlijst","Naam",{newList=false}){name->val id=store.addList(name);select(id);newList=false}
}

@Composable fun FrequentRow(product:Product,add:(String)->Unit){var qty by remember(product.name){mutableStateOf(product.quantity)};Surface(shape=RoundedCornerShape(18.dp),color=PaleGreen,modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(product.name,fontWeight=FontWeight.SemiBold);Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(qty,{qty=it},label={Text("Hoeveel")},singleLine=true,modifier=Modifier.weight(1f));Spacer(Modifier.width(8.dp));FilledIconButton({add(qty)},modifier=Modifier.size(52.dp)){Icon(Icons.Default.Add,"Direct toevoegen")}}}}}

@Composable fun SectionTitle(text:String,count:Int){Row(Modifier.padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text(text,fontWeight=FontWeight.Bold,fontSize=23.sp);Spacer(Modifier.width(10.dp));Surface(color=Color(0xFFE8E8E8),shape=CircleShape){Text("$count",Modifier.padding(horizontal=11.dp,vertical=5.dp),color=Color.DarkGray)}}}

@Composable
fun GroceryCard(rows:List<Grocery>,toggle:(Long,Long)->Unit,delete:(Long,Long)->Unit,quantity:(Long,String)->Unit,move:((Long,Int)->Unit)?) {
    if(rows.isEmpty()){Text("Nog geen producten.",color=Color.Gray,modifier=Modifier.padding(horizontal=24.dp,vertical=12.dp));return}
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val rowStepPx = 112f
    Surface(Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=Color.White,shadowElevation=1.dp){
        Column {
            rows.forEachIndexed { i,g ->
                key(g.id) {
                    GroceryRow(
                        g=g,
                        isDragging=draggingId==g.id,
                        toggle={toggle(g.id,0L)},
                        delete={delete(g.id,0L)},
                        saveQty={quantity(g.id,it)},
                        dragEnabled=move!=null,
                        onDragStart={ draggingId=g.id; dragY=0f },
                        onDrag={dy ->
                            if(move!=null){
                                dragY += dy
                                if(dragY > rowStepPx && i < rows.lastIndex){ move(g.id,1); dragY=0f }
                                else if(dragY < -rowStepPx && i > 0){ move(g.id,-1); dragY=0f }
                            }
                        },
                        onDragEnd={ draggingId=null; dragY=0f }
                    )
                }
                if(i<rows.lastIndex) HorizontalDivider(Modifier.padding(start=58.dp),color=Color(0xFFE8E8E8))
            }
        }
    }
}

@Composable
fun GroceryRow(
    g:Grocery,
    isDragging:Boolean,
    toggle:()->Unit,
    delete:()->Unit,
    saveQty:(String)->Unit,
    dragEnabled:Boolean,
    onDragStart:()->Unit,
    onDrag:(Float)->Unit,
    onDragEnd:()->Unit
) {
    var qty by remember(g.id,g.quantity){mutableStateOf(g.quantity)}
    val dragModifier = if(dragEnabled) Modifier.pointerInput(g.id) {
        detectDragGesturesAfterLongPress(
            onDragStart={onDragStart()},
            onDragEnd=onDragEnd,
            onDragCancel=onDragEnd,
            onDrag={change,amount->change.consume();onDrag(amount.y)}
        )
    } else Modifier
    Row(
        Modifier.fillMaxWidth().then(dragModifier).graphicsLayer { alpha=if(isDragging)0.65f else 1f }.background(if(isDragging)PaleGreen else Color.Transparent).padding(horizontal=8.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        IconButton(toggle,modifier=Modifier.size(50.dp)){Icon(if(g.checked)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,"Afvinken",tint=Green,modifier=Modifier.size(35.dp))}
        Column(Modifier.weight(1f)){
            Text(g.name,fontSize=18.sp,fontWeight=FontWeight.Medium,textDecoration=if(g.checked)TextDecoration.LineThrough else null,color=if(g.checked)Color.Gray else Ink)
            OutlinedTextField(qty,{qty=it;saveQty(it)},label={Text("Hoeveel")},singleLine=true,modifier=Modifier.fillMaxWidth())
        }
        if(dragEnabled) Icon(Icons.Default.DragHandle,"Ingedrukt houden en slepen",tint=Color.Gray,modifier=Modifier.padding(horizontal=6.dp))
        IconButton(delete){Icon(Icons.Default.DeleteOutline,"Verwijder",tint=Color.Gray)}
    }
}

@Composable
fun ProductPicker(products:List<Product>,dismiss:()->Unit,add:(List<Pair<Product,String>>)->Unit){val selected=remember{mutableStateMapOf<String,String>()};AlertDialog(onDismissRequest=dismiss,title={Text("Kies producten")},text={LazyColumn(Modifier.heightIn(max=460.dp)){items(products.sortedBy{it.name},key={it.name}){p->var qty by remember(p.name){mutableStateOf(p.quantity)};Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(selected.containsKey(p.name),{v->if(v)selected[p.name]=qty else selected.remove(p.name)});Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Medium);OutlinedTextField(qty,{qty=it;if(selected.containsKey(p.name))selected[p.name]=it},label={Text("Hoeveel")},singleLine=true)}}}}},confirmButton={Button({add(products.filter{selected.containsKey(it.name)}.map{it to (selected[it.name]?:it.quantity)})},enabled=selected.isNotEmpty()){Text("Toevoegen (${selected.size})")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}

@Composable
fun WeekMenuPage(store:AppStore,selectedId:Long){val list=store.lists.firstOrNull{it.id==selectedId}?:store.lists.first();LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Weekmenu")};item{Text("Tik in een vak om direct te typen. Producten kun je kiezen en meteen op ${list.name} zetten.",color=Color.Gray,modifier=Modifier.padding(horizontal=20.dp,vertical=4.dp))};items(store.menu,key={it.day}){day->MenuCard(day,store.products,{m,i->store.updateMenu(day.day,m,i)},{chosen->chosen.forEach{(p,q)->store.addGrocery(list.id,p.name,q)}})}}}

@Composable fun MenuCard(day:MenuDay,products:List<Product>,update:(String,String)->Unit,add:(List<Pair<Product,String>>)->Unit){var meal by remember(day.day,day.meal){mutableStateOf(day.meal)};var ingredients by remember(day.day,day.ingredients){mutableStateOf(day.ingredients)};var picker by remember{mutableStateOf(false)};Surface(Modifier.padding(horizontal=20.dp,vertical=7.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=Color.White,shadowElevation=1.dp){Column(Modifier.padding(16.dp)){Text(day.day,fontWeight=FontWeight.Bold,fontSize=20.sp);OutlinedTextField(meal,{meal=it;update(meal,ingredients)},label={Text("Wat eten we?")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));OutlinedTextField(ingredients,{ingredients=it;update(meal,ingredients)},label={Text("Notitie of ingrediënten")},modifier=Modifier.fillMaxWidth());OutlinedButton({picker=true},modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("Kies producten voor boodschappenlijst")}}};if(picker)ProductPicker(products,{picker=false}){add(it);picker=false}}

@Composable
fun ListsPage(store:AppStore,selectedId:Long,select:(Long)->Unit){var newList by remember{mutableStateOf(false)};var newProduct by remember{mutableStateOf(false)};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)){item{Header("Lijsten")};item{Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({newList=true},modifier=Modifier.weight(1f)){Text("Nieuwe lijst")};OutlinedButton({newProduct=true},modifier=Modifier.weight(1f)){Text("Nieuw product")}}};item{Text("Boodschappenlijstjes",fontWeight=FontWeight.Bold,fontSize=22.sp,modifier=Modifier.padding(20.dp))};items(store.lists,key={it.id}){l->ListItem(headlineContent={Text(l.name,fontWeight=FontWeight.Bold)},supportingContent={Text("${l.items.count{!it.checked}} op lijstje")},leadingContent={Icon(Icons.Default.List,null,tint=Green)},trailingContent={if(store.lists.size>1)IconButton({store.deleteList(l.id)}){Icon(Icons.Default.DeleteOutline,null)}},modifier=Modifier.clickable{select(l.id)},colors=ListItemDefaults.colors(containerColor=if(l.id==selectedId)PaleGreen else Color.Transparent))};item{Text("Vaste producten",fontWeight=FontWeight.Bold,fontSize=22.sp,modifier=Modifier.padding(20.dp))};items(store.products.sortedByDescending{it.uses},key={it.name}){p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(p.quantity.ifBlank{"Geen vaste hoeveelheid"})},trailingContent={IconButton({store.deleteProduct(p.name)}){Icon(Icons.Default.DeleteOutline,null)}},colors=ListItemDefaults.colors(containerColor=Color.Transparent))}}
    if(newList)NameDialog("Nieuwe lijst","Naam",{newList=false}){store.addList(it);newList=false};if(newProduct)ProductDialog({newProduct=false}){n,q->store.addProduct(n,q);newProduct=false}}

@Composable fun NameDialog(title:String,label:String,dismiss:()->Unit,save:(String)->Unit){var text by remember{mutableStateOf("")};AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={OutlinedTextField(text,{text=it},label={Text(label)},singleLine=true)},confirmButton={Button({save(text.trim())},enabled=text.isNotBlank()){Text("Opslaan")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}
@Composable fun ProductDialog(dismiss:()->Unit,save:(String,String)->Unit){var name by remember{mutableStateOf("")};var qty by remember{mutableStateOf("")};AlertDialog(onDismissRequest=dismiss,title={Text("Nieuw product")},text={Column{OutlinedTextField(name,{name=it},label={Text("Product")});OutlinedTextField(qty,{qty=it},label={Text("Vaste hoeveelheid, bijvoorbeeld 1 pak")})}},confirmButton={Button({save(name.trim(),qty.trim())},enabled=name.isNotBlank()){Text("Opslaan")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}
@Composable fun BottomBar(tab:Int,change:(Int)->Unit){Surface(color=Color.White,shadowElevation=10.dp,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)){NavigationBar(containerColor=Color.Transparent){listOf(Triple("Boodschappen",Icons.Default.ShoppingBasket,0),Triple("Weekmenu",Icons.Default.CalendarMonth,1),Triple("Lijsten",Icons.Default.Checklist,2)).forEach{(label,icon,i)->NavigationBarItem(tab==i,{change(i)},{Icon(icon,null)},label={Text(label)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Green,selectedTextColor=Green,indicatorColor=PaleGreen))}}}}

class AppStore(private val context:Context){
    private val prefs=context.getSharedPreferences("grocery_v2",Context.MODE_PRIVATE)
    var lists by mutableStateOf<List<GroceryList>>(emptyList());private set
    var products by mutableStateOf<List<Product>>(emptyList());private set
    var menu by mutableStateOf<List<MenuDay>>(emptyList());private set
    init{load()}
    fun addList(name:String):Long{val id=System.currentTimeMillis();lists=lists+GroceryList(id,name);save();return id}
    fun deleteList(id:Long){lists=lists.filterNot{it.id==id};save()}
    fun addProduct(name:String,qty:String=""){if(products.none{it.name.equals(name,true)})products=products+Product(name,0,qty);save()}
    fun deleteProduct(name:String){products=products.filterNot{it.name==name};save()}
    fun addGrocery(listId:Long,name:String,qty:String){val clean=name.trim();if(clean.isBlank())return;val g=Grocery(System.nanoTime(),clean,qty.trim());lists=lists.map{if(it.id==listId)it.copy(items=it.items+g)else it};val found=products.firstOrNull{it.name.equals(clean,true)};products=if(found==null)products+Product(clean,1,qty.trim())else products.map{if(it.name.equals(clean,true))it.copy(uses=it.uses+1,quantity=qty.trim().ifBlank{it.quantity})else it};save()}
    private fun locate(id:Long)=lists.firstNotNullOfOrNull{l->l.items.firstOrNull{it.id==id}?.let{l.id}}
    fun toggleGrocery(id:Long,unused:Long){val listId=locate(id)?:return;lists=lists.map{l->if(l.id==listId)l.copy(items=l.items.map{if(it.id==id)it.copy(checked=!it.checked)else it})else l};save()}
    fun deleteGrocery(id:Long,unused:Long){val listId=locate(id)?:return;lists=lists.map{if(it.id==listId)it.copy(items=it.items.filterNot{g->g.id==id})else it};save()}
    fun updateQuantity(listId:Long,id:Long,q:String){lists=lists.map{if(it.id==listId)it.copy(items=it.items.map{g->if(g.id==id)g.copy(quantity=q)else g})else it};save()}
    fun moveGrocery(listId:Long,id:Long,d:Int){lists=lists.map{l->if(l.id!=listId)l else{val m=l.items.toMutableList();val i=m.indexOfFirst{it.id==id};if(i<0)l else{val j=(i+d).coerceIn(0,m.lastIndex);if(i!=j){val x=m.removeAt(i);m.add(j,x)};l.copy(items=m)}}};save()}
    fun clearChecked(listId:Long){lists=lists.map{if(it.id==listId)it.copy(items=it.items.filterNot{g->g.checked})else it};save()}
    fun updateMenu(day:String,meal:String,ingredients:String){menu=menu.map{if(it.day==day)it.copy(meal=meal,ingredients=ingredients)else it};save()}
    private fun save(){val root=JSONObject();root.put("lists",JSONArray().apply{lists.forEach{l->put(JSONObject().put("id",l.id).put("name",l.name).put("items",JSONArray().apply{l.items.forEach{g->put(JSONObject().put("id",g.id).put("name",g.name).put("quantity",g.quantity).put("checked",g.checked))}}))}});root.put("products",JSONArray().apply{products.forEach{put(JSONObject().put("name",it.name).put("uses",it.uses).put("quantity",it.quantity))}});root.put("menu",JSONArray().apply{menu.forEach{put(JSONObject().put("day",it.day).put("meal",it.meal).put("ingredients",it.ingredients))}});prefs.edit().putString("data",root.toString()).apply()}
    private fun load(){try{prefs.getString("data",null)?.let{raw->val root=JSONObject(raw);lists=(0 until root.getJSONArray("lists").length()).map{i->val o=root.getJSONArray("lists").getJSONObject(i);val a=o.getJSONArray("items");GroceryList(o.getLong("id"),o.getString("name"),(0 until a.length()).map{j->val g=a.getJSONObject(j);Grocery(g.getLong("id"),g.getString("name"),g.optString("quantity"),g.optBoolean("checked"))})};products=(0 until root.getJSONArray("products").length()).map{i->val p=root.getJSONArray("products").getJSONObject(i);Product(p.getString("name"),p.optInt("uses"),p.optString("quantity"))};menu=(0 until root.getJSONArray("menu").length()).map{i->val m=root.getJSONArray("menu").getJSONObject(i);MenuDay(m.getString("day"),m.optString("meal"),m.optString("ingredients"))}}}catch(_:Exception){}
        if(lists.isEmpty())lists=listOf(GroceryList(1,"Weekboodschappen",listOf(Grocery(11,"Melk","1 pak"),Grocery(12,"Brood","1 brood"))))
        if(products.isEmpty())products=listOf(Product("Melk",8,"1 pak"),Product("Brood",7,"1 brood"),Product("Bananen",6,"6 stuks"),Product("Eieren",5,"10 stuks"),Product("Kaas",4,"1 stuk"),Product("Yoghurt",3,"1 liter"),Product("Pasta",2,"1 pak"),Product("Tomaten",1,"500 gr"))
        if(menu.isEmpty())menu=listOf("Maandag","Dinsdag","Woensdag","Donderdag","Vrijdag","Zaterdag","Zondag").map{MenuDay(it)}
    }
}
