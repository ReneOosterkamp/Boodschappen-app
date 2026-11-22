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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

private val Green=Color(0xFF087A43);private val Pale=Color(0xFFEAF6EE);private val Canvas=Color(0xFFF7F7F5);private val Ink=Color(0xFF101820)
data class Product(val name:String,val uses:Int=0,val quantity:String="")
data class Grocery(val id:Long,val name:String,val quantity:String="",val checked:Boolean=false)
data class GroceryList(val id:Long,val name:String,val items:List<Grocery> = emptyList())
data class MenuDay(val day:String,val meal:String="",val ingredients:String="")
data class WeekMenu(val id:Long,val name:String,val days:List<MenuDay>)
private val dayNames=listOf("Maandag","Dinsdag","Woensdag","Donderdag","Vrijdag","Zaterdag","Zondag")

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{AppTheme{GroceryApp(this)}}}}
@Composable private fun AppTheme(content:@Composable ()->Unit){MaterialTheme(colorScheme=lightColorScheme(primary=Green,background=Canvas,surface=Color.White,onBackground=Ink),typography=Typography(headlineLarge=MaterialTheme.typography.headlineLarge.copy(fontSize=32.sp,fontWeight=FontWeight.Bold),titleLarge=MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.Bold)),content=content)}

@Composable fun GroceryApp(context:Context){val store=remember{AppStore(context)};var tab by remember{mutableIntStateOf(0)};var listId by remember{mutableLongStateOf(store.lists.first().id)};Scaffold(containerColor=Canvas,bottomBar={BottomBar(tab){tab=it}}){pad->Box(Modifier.padding(pad)){when(tab){0->ShoppingScreen(store,listId,{listId=it});1->WeekMenuScreen(store,listId);else->ListsScreen(store,listId,{listId=it})}}}}
@Composable fun PageTitle(title:String){Text(title,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(horizontal=18.dp,vertical=12.dp))}

@Composable fun ShoppingScreen(store:AppStore,listId:Long,select:(Long)->Unit){val list=store.lists.firstOrNull{it.id==listId}?:store.lists.first();var chooseList by remember{mutableStateOf(false)};var addProduct by remember{mutableStateOf(false)};var newList by remember{mutableStateOf(false)};var showDone by remember{mutableStateOf(false)};val open=list.items.filterNot{it.checked};val done=list.items.filter{it.checked}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=18.dp)){
        item{PageTitle("Boodschappen")}
        item{Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Surface(shape=RoundedCornerShape(16.dp),color=Color.White,modifier=Modifier.fillMaxWidth().clickable{chooseList=true}){Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Text(list.name,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Icon(Icons.Default.KeyboardArrowDown,null)}};DropdownMenu(chooseList,{chooseList=false}){store.lists.forEach{l->DropdownMenuItem({Text(l.name)},{select(l.id);chooseList=false})}}};Spacer(Modifier.width(8.dp));FilledIconButton({newList=true}){Icon(Icons.Default.Add,"Nieuwe lijst")}}
        }
        item{OutlinedButton({addProduct=true},modifier=Modifier.padding(16.dp).fillMaxWidth(),shape=RoundedCornerShape(15.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Product toevoegen…",modifier=Modifier.weight(1f))}}
        item{CompactHeader("Lijstje",open.size,false){} }
        item{CompactGroceryList(open,store::toggle,store::remove,store::setQuantity){id,steps->store.moveByOffset(list.id,id,steps)}}
        if(done.isNotEmpty()){item{CompactHeader("Gepakt",done.size,!showDone){showDone=!showDone}};if(showDone)item{CompactGroceryList(done,store::toggle,store::remove,store::setQuantity,null)};if(showDone)item{TextButton({store.clearDone(list.id)},Modifier.fillMaxWidth()){Text("Afgevinkte verwijderen")}}}
    }
    if(addProduct)AddProductDialog(store.products,{addProduct=false}){name,qty->store.addGrocery(list.id,name,qty);addProduct=false}
    if(newList)TextDialog("Nieuwe boodschappenlijst","Naam",{newList=false}){select(store.addList(it));newList=false}
}

@Composable fun CompactHeader(text:String,count:Int,collapsed:Boolean,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable{onClick()}.padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text("$text ($count)",fontWeight=FontWeight.Bold,fontSize=20.sp,modifier=Modifier.weight(1f));if(text=="Gepakt")Icon(if(collapsed)Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,null)}}
@Composable
fun CompactGroceryList(
    rows:List<Grocery>,
    toggle:(Long)->Unit,
    remove:(Long)->Unit,
    setQty:(Long,String)->Unit,
    reorder:((Long,Int)->Unit)?
){
    if(rows.isEmpty()){Text("Nog geen producten",color=Color.Gray,modifier=Modifier.padding(20.dp));return}
    Surface(Modifier.padding(horizontal=16.dp).fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=Color.White){
        Column{
            rows.forEachIndexed{i,g->
                key(g.id){CompactProductRow(g,{toggle(g.id)},{remove(g.id)},{setQty(g.id,it)},reorder?.let{{steps->it(g.id,steps)}})}
                if(i<rows.lastIndex)HorizontalDivider(Modifier.padding(start=50.dp),color=Color(0xFFE9E9E9))
            }
        }
    }
}

@Composable
fun CompactProductRow(
    g:Grocery,
    toggle:()->Unit,
    remove:()->Unit,
    qty:(String)->Unit,
    reorder:((Int)->Unit)?
){
    val haptics=LocalHapticFeedback.current
    var dragging by remember{mutableStateOf(false)}
    var dragTotal by remember{mutableFloatStateOf(0f)}
    var editQty by remember{mutableStateOf(false)}
    var q by remember(g.id,g.quantity){mutableStateOf(g.quantity)}
    val rowHeightPx=52f
    val dragMod=if(reorder!=null)Modifier.pointerInput(g.id){
        detectDragGesturesAfterLongPress(
            onDragStart={dragging=true;dragTotal=0f;haptics.performHapticFeedback(HapticFeedbackType.LongPress)},
            onDragCancel={dragging=false;dragTotal=0f},
            onDragEnd={
                val steps=(dragTotal/rowHeightPx).toInt()
                if(steps!=0){reorder(steps);haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)}
                dragging=false;dragTotal=0f
            },
            onDrag={change,amount->change.consume();dragTotal+=amount.y}
        )
    }else Modifier
    Row(
        Modifier.fillMaxWidth().then(dragMod).graphicsLayer{alpha=if(dragging).65f else 1f;translationY=if(dragging)dragTotal else 0f}
            .background(if(dragging)Pale else Color.Transparent).heightIn(min=52.dp).padding(horizontal=6.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        IconButton(toggle,Modifier.size(44.dp)){Icon(if(g.checked)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null,tint=Green)}
        Text(g.name,modifier=Modifier.weight(1f).clickable{toggle()},maxLines=1,textDecoration=if(g.checked)TextDecoration.LineThrough else null,fontSize=16.sp)
        Text(g.quantity.ifBlank{"Hoeveel"},color=if(g.quantity.isBlank())Color.Gray else Ink,fontSize=14.sp,maxLines=1,modifier=Modifier.clickable{editQty=true}.padding(horizontal=8.dp,vertical=14.dp))
        if(reorder!=null)Icon(Icons.Default.DragHandle,"Vasthouden en slepen",tint=Color.Gray,modifier=Modifier.padding(horizontal=4.dp))
        IconButton(remove,Modifier.size(40.dp)){Icon(Icons.Default.Close,"Verwijderen",tint=Color.Gray)}
    }
    if(editQty)AlertDialog(onDismissRequest={editQty=false},title={Text(g.name)},text={OutlinedTextField(q,{q=it},label={Text("Hoeveelheid")},singleLine=true)},confirmButton={Button({qty(q);editQty=false}){Text("Opslaan")}},dismissButton={TextButton({editQty=false}){Text("Annuleren")}})
}

@Composable fun AddProductDialog(products:List<Product>,dismiss:()->Unit,add:(String,String)->Unit){var search by remember{mutableStateOf("")};var qty by remember{mutableStateOf("")};val matches=products.filter{search.isBlank()||it.name.contains(search,true)}.sortedWith(compareByDescending<Product>{it.uses}.thenBy{it.name});AlertDialog(onDismissRequest=dismiss,title={Text("Product toevoegen")},text={Column{OutlinedTextField(search,{search=it},label={Text("Zoek of typ product")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(qty,{qty=it},label={Text("Hoeveel, bijvoorbeeld 500 gr")},singleLine=true,modifier=Modifier.fillMaxWidth());Text("Vaak gebruikt / producten",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp));LazyColumn(Modifier.heightIn(max=260.dp)){items(matches.take(15),key={it.name}){p->ListItem(headlineContent={Text(p.name)},trailingContent={Text(p.quantity)},modifier=Modifier.clickable{add(p.name,qty.ifBlank{p.quantity})})}}}},confirmButton={Button({add(search.trim(),qty.trim())},enabled=search.isNotBlank()){Text("Toevoegen")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}

@Composable fun WeekMenuScreen(store:AppStore,listId:Long){val list=store.lists.firstOrNull{it.id==listId}?:store.lists.first();var menuId by remember{mutableLongStateOf(store.weekMenus.first().id)};val menu=store.weekMenus.firstOrNull{it.id==menuId}?:store.weekMenus.first();var choose by remember{mutableStateOf(false)};var editDay by remember{mutableStateOf<MenuDay?>(null)};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=18.dp)){item{PageTitle("Weekmenu")};item{Box(Modifier.padding(horizontal=16.dp)){Surface(shape=RoundedCornerShape(16.dp),color=Color.White,modifier=Modifier.fillMaxWidth().clickable{choose=true}){Row(Modifier.padding(14.dp)){Text(menu.name,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Icon(Icons.Default.KeyboardArrowDown,null)}};DropdownMenu(choose,{choose=false}){store.weekMenus.forEach{m->DropdownMenuItem({Text(m.name)},{menuId=m.id;choose=false})}}}};item{Spacer(Modifier.height(10.dp))};items(menu.days,key={it.day}){d->Surface(color=Color.White,modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth().clickable{editDay=d}){Row(Modifier.height(58.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text(d.day.take(2),fontWeight=FontWeight.Bold,modifier=Modifier.width(42.dp));Text(d.meal.ifBlank{"+ Gerecht kiezen"},color=if(d.meal.isBlank())Green else Ink,modifier=Modifier.weight(1f),maxLines=1);Icon(Icons.Default.KeyboardArrowRight,null)}};HorizontalDivider(Modifier.padding(horizontal=16.dp),color=Color(0xFFE9E9E9))}}
    editDay?.let{d->DayDialog(d,store.products,{editDay=null}){meal,ingredients,chosen->store.updateDay(menu.id,d.day,meal,ingredients);chosen.forEach{(p,q)->store.addGrocery(list.id,p.name,q)};editDay=null}}
}
@Composable fun DayDialog(day:MenuDay,products:List<Product>,dismiss:()->Unit,save:(String,String,List<Pair<Product,String>>)->Unit){var meal by remember{mutableStateOf(day.meal)};var notes by remember{mutableStateOf(day.ingredients)};var picker by remember{mutableStateOf(false)};var chosen by remember{mutableStateOf<List<Pair<Product,String>>>(emptyList())};AlertDialog(onDismissRequest=dismiss,title={Text(day.day)},text={Column{OutlinedTextField(meal,{meal=it},label={Text("Gerecht")},modifier=Modifier.fillMaxWidth());OutlinedTextField(notes,{notes=it},label={Text("Notitie")},modifier=Modifier.fillMaxWidth());OutlinedButton({picker=true},Modifier.fillMaxWidth().padding(top=8.dp)){Text("Producten kiezen (${chosen.size})")}}},confirmButton={Button({save(meal,notes,chosen)}){Text("Opslaan")}},dismissButton={TextButton(dismiss){Text("Annuleren")}});if(picker)MultiProductDialog(products,{picker=false}){chosen=it;picker=false}}
@Composable fun MultiProductDialog(products:List<Product>,dismiss:()->Unit,save:(List<Pair<Product,String>>)->Unit){val selected=remember{mutableStateMapOf<String,String>()};AlertDialog(onDismissRequest=dismiss,title={Text("Producten kiezen")},text={LazyColumn(Modifier.heightIn(max=420.dp)){items(products.sortedBy{it.name},key={it.name}){p->var q by remember(p.name){mutableStateOf(p.quantity)};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(selected.containsKey(p.name),{v->if(v)selected[p.name]=q else selected.remove(p.name)});Column(Modifier.weight(1f)){Text(p.name);OutlinedTextField(q,{q=it;if(selected.containsKey(p.name))selected[p.name]=it},label={Text("Hoeveel")},singleLine=true)}}}}},confirmButton={Button({save(products.filter{selected.containsKey(it.name)}.map{it to (selected[it.name]?:"")})}){Text("Kiezen")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}

@Composable
fun ListsScreen(store:AppStore,selectedId:Long,select:(Long)->Unit){
    var newList by remember{mutableStateOf(false)}
    var newProduct by remember{mutableStateOf(false)}
    var newMenu by remember{mutableStateOf(false)}
    var alphabetical by remember{mutableStateOf(true)}
    val shownProducts=if(alphabetical)store.products.sortedBy{it.name.lowercase(Locale("nl","NL"))}else store.products.sortedByDescending{it.uses}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=18.dp)){
        item{PageTitle("Lijsten")}
        item{ManageTitle("Boodschappenlijsten")}
        items(store.lists,key={it.id}){l->ManageRow(l.name,"${l.items.count{!it.checked}}",{select(l.id)},if(store.lists.size>1){{store.deleteList(l.id)}}else null)}
        item{AddRow("Nieuwe boodschappenlijst"){newList=true}}
        item{ManageTitle("Weekmenu's")}
        items(store.weekMenus,key={it.id}){m->ManageRow(m.name,"7 dagen",{},if(store.weekMenus.size>1){{store.deleteWeekMenu(m.id)}}else null)}
        item{AddRow("Nieuw weekmenu"){newMenu=true}}
        item{
            Row(Modifier.fillMaxWidth().padding(start=18.dp,top=20.dp,end=10.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){
                Text("Producten",fontWeight=FontWeight.Bold,fontSize=21.sp,modifier=Modifier.weight(1f))
                FilterChip(selected=alphabetical,onClick={alphabetical=true},label={Text("A–Z")})
                Spacer(Modifier.width(6.dp))
                FilterChip(selected=!alphabetical,onClick={alphabetical=false},label={Text("Vaak")})
            }
        }
        items(shownProducts,key={it.name}){p->ManageRow(p.name,p.quantity,{}, {store.deleteProduct(p.name)})}
        item{AddRow("Nieuw product"){newProduct=true}}
    }
    if(newList)TextDialog("Nieuwe boodschappenlijst","Naam",{newList=false}){store.addList(it);newList=false}
    if(newMenu)TextDialog("Nieuw weekmenu","Naam",{newMenu=false}){store.addWeekMenu(it);newMenu=false}
    if(newProduct)ProductDialog({newProduct=false}){n,q->store.addProduct(n,q);newProduct=false}
}

@Composable fun ManageTitle(text:String){Text(text,fontWeight=FontWeight.Bold,fontSize=21.sp,modifier=Modifier.padding(start=18.dp,top=20.dp,bottom=8.dp))}
@Composable fun ManageRow(name:String,detail:String,onClick:()->Unit,onDelete:(()->Unit)?){ListItem(headlineContent={Text(name)},trailingContent={Row(verticalAlignment=Alignment.CenterVertically){Text(detail,color=Color.Gray);if(onDelete!=null)IconButton(onDelete){Icon(Icons.Default.DeleteOutline,null)}}},modifier=Modifier.clickable{onClick()},colors=ListItemDefaults.colors(containerColor=Color.White))}
@Composable fun AddRow(text:String,onClick:()->Unit){ListItem(headlineContent={Text(text,color=Green,fontWeight=FontWeight.Bold)},leadingContent={Icon(Icons.Default.Add,null,tint=Green)},modifier=Modifier.clickable{onClick()},colors=ListItemDefaults.colors(containerColor=Color.Transparent))}
@Composable fun TextDialog(title:String,label:String,dismiss:()->Unit,save:(String)->Unit){var text by remember{mutableStateOf("")};AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={OutlinedTextField(text,{text=it},label={Text(label)})},confirmButton={Button({save(text.trim())},enabled=text.isNotBlank()){Text("Opslaan")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}
@Composable fun ProductDialog(dismiss:()->Unit,save:(String,String)->Unit){var n by remember{mutableStateOf("")};var q by remember{mutableStateOf("")};AlertDialog(onDismissRequest=dismiss,title={Text("Nieuw product")},text={Column{OutlinedTextField(n,{n=it},label={Text("Product")});OutlinedTextField(q,{q=it},label={Text("Standaardhoeveelheid")})}},confirmButton={Button({save(n.trim(),q.trim())},enabled=n.isNotBlank()){Text("Opslaan")}},dismissButton={TextButton(dismiss){Text("Annuleren")}})}
@Composable fun BottomBar(tab:Int,change:(Int)->Unit){NavigationBar(containerColor=Color.White){listOf(Triple("Boodschappen",Icons.Default.ShoppingCart,0),Triple("Weekmenu",Icons.Default.Restaurant,1),Triple("Lijsten",Icons.Default.List,2)).forEach{(t,i,n)->NavigationBarItem(tab==n,{change(n)},{Icon(i,null)},label={Text(t)},colors=NavigationBarItemDefaults.colors(indicatorColor=Pale,selectedIconColor=Green,selectedTextColor=Green))}}}

class AppStore(private val c:Context){private val p=c.getSharedPreferences("grocery_v2",Context.MODE_PRIVATE);var lists by mutableStateOf<List<GroceryList>>(emptyList());private set;var products by mutableStateOf<List<Product>>(emptyList());private set;var weekMenus by mutableStateOf<List<WeekMenu>>(emptyList());private set
    init{load()};fun addList(n:String):Long{val id=System.currentTimeMillis();lists=lists+GroceryList(id,n);save();return id};fun deleteList(id:Long){lists=lists.filterNot{it.id==id};save()};fun addProduct(n:String,q:String){if(products.none{it.name.equals(n,true)})products=products+Product(n,0,q);save()};fun deleteProduct(n:String){products=products.filterNot{it.name==n};save()};fun addWeekMenu(n:String){weekMenus=weekMenus+WeekMenu(System.currentTimeMillis(),n,dayNames.map{MenuDay(it)});save()};fun deleteWeekMenu(id:Long){weekMenus=weekMenus.filterNot{it.id==id};save()}
    fun addGrocery(lid:Long,n:String,q:String){if(n.isBlank())return;val g=Grocery(System.nanoTime(),n.trim(),q.trim());lists=lists.map{if(it.id==lid)it.copy(items=it.items+g)else it};val old=products.firstOrNull{it.name.equals(n,true)};products=if(old==null)products+Product(n.trim(),1,q.trim())else products.map{if(it==old)it.copy(uses=it.uses+1,quantity=q.ifBlank{it.quantity})else it};save()}
    private fun find(id:Long)=lists.firstNotNullOfOrNull{l->l.items.firstOrNull{it.id==id}?.let{l.id}};fun toggle(id:Long){val l=find(id)?:return;lists=lists.map{if(it.id==l)it.copy(items=it.items.map{g->if(g.id==id)g.copy(checked=!g.checked)else g})else it};save()};fun remove(id:Long){val l=find(id)?:return;lists=lists.map{if(it.id==l)it.copy(items=it.items.filterNot{g->g.id==id})else it};save()};fun setQuantity(id:Long,q:String){val l=find(id)?:return;lists=lists.map{if(it.id==l)it.copy(items=it.items.map{g->if(g.id==id)g.copy(quantity=q)else g})else it};save()};fun moveByOffset(lid:Long,id:Long,steps:Int){lists=lists.map{l->if(l.id!=lid)l else{val open=l.items.filterNot{it.checked}.toMutableList();val done=l.items.filter{it.checked};val from=open.indexOfFirst{it.id==id};if(from>=0){val item=open.removeAt(from);val target=(from+steps).coerceIn(0,open.size);open.add(target,item)};l.copy(items=open+done)}};save()};fun clearDone(lid:Long){lists=lists.map{if(it.id==lid)it.copy(items=it.items.filterNot{g->g.checked})else it};save()};fun updateDay(mid:Long,day:String,meal:String,notes:String){weekMenus=weekMenus.map{m->if(m.id==mid)m.copy(days=m.days.map{if(it.day==day)it.copy(meal=meal,ingredients=notes)else it})else m};save()}
    private fun save(){val r=JSONObject();r.put("lists",JSONArray().apply{lists.forEach{l->put(JSONObject().put("id",l.id).put("name",l.name).put("items",JSONArray().apply{l.items.forEach{g->put(JSONObject().put("id",g.id).put("name",g.name).put("quantity",g.quantity).put("checked",g.checked))}}))}});r.put("products",JSONArray().apply{products.forEach{put(JSONObject().put("name",it.name).put("uses",it.uses).put("quantity",it.quantity))}});r.put("weekMenus",JSONArray().apply{weekMenus.forEach{m->put(JSONObject().put("id",m.id).put("name",m.name).put("days",JSONArray().apply{m.days.forEach{d->put(JSONObject().put("day",d.day).put("meal",d.meal).put("ingredients",d.ingredients))}}))}});p.edit().putString("data_v6",r.toString()).apply()}
    private fun load(){try{p.getString("data_v6",null)?.let{raw->val r=JSONObject(raw);val la=r.getJSONArray("lists");lists=(0 until la.length()).map{i->val o=la.getJSONObject(i);val a=o.getJSONArray("items");GroceryList(o.getLong("id"),o.getString("name"),(0 until a.length()).map{j->val g=a.getJSONObject(j);Grocery(g.getLong("id"),g.getString("name"),g.optString("quantity"),g.optBoolean("checked"))})};val pa=r.getJSONArray("products");products=(0 until pa.length()).map{i->val o=pa.getJSONObject(i);Product(o.getString("name"),o.optInt("uses"),o.optString("quantity"))};val ma=r.getJSONArray("weekMenus");weekMenus=(0 until ma.length()).map{i->val o=ma.getJSONObject(i);val a=o.getJSONArray("days");WeekMenu(o.getLong("id"),o.getString("name"),(0 until a.length()).map{j->val d=a.getJSONObject(j);MenuDay(d.getString("day"),d.optString("meal"),d.optString("ingredients"))})}}}catch(_:Exception){};if(lists.isEmpty())lists=listOf(GroceryList(1,"Weekboodschappen",listOf(Grocery(11,"Melk","1 pak"),Grocery(12,"Brood","1 brood"))));if(products.isEmpty())products=listOf(Product("Melk",8,"1 pak"),Product("Brood",7,"1 brood"),Product("Gehakt",6,"500 gr"),Product("Bananen",5,"6 stuks"));if(weekMenus.isEmpty())weekMenus=listOf(WeekMenu(1,"Deze week",dayNames.map{MenuDay(it)}))}
}
