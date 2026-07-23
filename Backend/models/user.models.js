const mongoose=require('mongoose');
const user_schema=new mongoose.Schema({
name:String,
email:{
type:String,
required:true,
unique:true,
},
   fcmToken: {
        type: String,
        default: null
    },

password:{
type:String,
required:true
},
role:{
    type:String,
enum:["user","admin"],
default:"user",
}
})
module.exports=mongoose.model("User",user_schema);