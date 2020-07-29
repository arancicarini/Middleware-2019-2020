const express = require('express');

const app = express();

/*app.use((req, res, next) => {
    res.status(200).json({
        message: "tutto appo!"
    });

});*/

app.get("/users", (req, res, next) => {
    res.json(["Tony","Lisa","Michael","Ginger","Food"]);
   });


const port = process.env.PORT || 3000;


app.listen(port, ()=>{console.log("Server running at port", port);});

module.exports = app;