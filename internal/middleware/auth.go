package authmw

import (
	"net/http"
	"strconv"

	"github.com/golang-jwt/jwt/v5"
	"github.com/labstack/echo/v5"
	echojwt "github.com/labstack/echo-jwt/v5"
)

func JWTMiddleware(secret string) echo.MiddlewareFunc {
	return echojwt.WithConfig(echojwt.Config{
		SigningKey:   []byte(secret),
		TokenLookup: "cookie:access_token",
		SuccessHandler: func(c *echo.Context) error {
			raw := c.Get("user")
			token, ok := raw.(*jwt.Token)
			if !ok || token == nil {
				return nil
			}
			claims, ok := token.Claims.(jwt.MapClaims)
			if !ok {
				return nil
			}
			if tokenType, _ := claims["type"].(string); tokenType != "access" {
				return nil
			}
			if iss, _ := claims["iss"].(string); iss != "gitrank" {
				return nil
			}
			sub, _ := claims["sub"].(string)
			login, _ := claims["login"].(string)
			userID, err := strconv.ParseInt(sub, 10, 64)
			if err != nil {
				return nil
			}
			c.Set("user_id", userID)
			c.Set("login", login)
			return nil
		},
	})
}

func RequireAuth() echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c *echo.Context) error {
			userID, _ := echo.ContextGet[int64](c, "user_id")
			if userID == 0 {
				return c.JSON(http.StatusUnauthorized, map[string]string{"message": "authentication required"})
			}
			return next(c)
		}
	}
}
