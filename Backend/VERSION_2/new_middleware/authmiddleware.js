const jwt = require('jsonwebtoken');

// 1. Protect routes (Verify JWT Token)
const authmiddleware = (req, res, next) => {
  try {
    const authHeader = req.header('Authorization');

    if (!authHeader) {
      return res.status(401).json({
        success: false,
        message: 'Token does not exist',
      });
    }

    const token = authHeader.split(' ')[1];

    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'Token does not exist',
      });
    }

    // Verify token and extract payload (e.g. { id, role, phoneNumber })
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;

    next();
  } catch (err) {
    console.log(err);

    return res.status(401).json({
      success: false,
      message: 'Invalid or expired token',
    });
  }
};

// 2. Authorize roles (RBAC)
const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: `User role '${req.user?.role}' is not authorized to perform this action`,
      });
    }
    next();
  };
};

module.exports = { authmiddleware, authorize };