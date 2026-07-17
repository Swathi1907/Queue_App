const jwt = require('jsonwebtoken');

const authmiddleware = (req, res, next) => {

    try {

        const authHeader =
            req.header('Authorization');

        if (!authHeader) {
            return res.status(401).json({
                message: "Token does not exist"
            });
        }

        const token =
            authHeader.split(' ')[1];

        console.log("SECRET =", process.env.JWT_SECRET);
        console.log("TOKEN =", token);
if(!token){
    return res.status(500).json({
        message:"Token does not exixt"
    })
}
        const decoded =
            jwt.verify(
                token,
                process.env.JWT_SECRET
            );

        req.user = decoded;

        next();

    } catch (err) {

        console.log(err);

        res.status(400).json({
            message: err.message
        });
    }
};

module.exports = authmiddleware;