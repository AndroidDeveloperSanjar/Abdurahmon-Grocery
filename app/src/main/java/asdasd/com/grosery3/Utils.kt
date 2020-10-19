package asdasd.com.grosery3

import android.app.Activity
import android.util.Log
import android.widget.Toast

fun Activity.toast(msg: String) = Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()

fun logging(msg: String) = Log.i("my_tag",msg)