package service

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"strconv"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type AuthService struct {
	jwtSecret     []byte
	encryptionKey []byte
}

func NewAuthService(jwtSecret string, encryptionKeyHex string) (*AuthService, error) {
	key, err := hex.DecodeString(encryptionKeyHex)
	if err != nil {
		return nil, fmt.Errorf("invalid encryption key hex: %w", err)
	}
	if len(key) != 32 {
		return nil, fmt.Errorf("encryption key must be 32 bytes, got %d", len(key))
	}
	return &AuthService{
		jwtSecret:     []byte(jwtSecret),
		encryptionKey: key,
	}, nil
}

func (s *AuthService) GenerateAccessToken(userID int64, login string) (string, error) {
	return s.generateToken(userID, login, "access", time.Hour)
}

func (s *AuthService) GenerateRefreshToken(userID int64, login string) (string, error) {
	return s.generateToken(userID, login, "refresh", 7*24*time.Hour)
}

func (s *AuthService) generateToken(userID int64, login string, tokenType string, expiry time.Duration) (string, error) {
	claims := jwt.MapClaims{
		"sub":   strconv.FormatInt(userID, 10),
		"login": login,
		"type":  tokenType,
		"iss":   "gitrank",
		"aud":   jwt.ClaimStrings{"gitrank"},
		"exp":   jwt.NewNumericDate(time.Now().Add(expiry)),
		"iat":   jwt.NewNumericDate(time.Now()),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(s.jwtSecret)
}

func (s *AuthService) ParseToken(tokenString string, expectedType string) (int64, string, error) {
	token, err := jwt.Parse(tokenString, func(t *jwt.Token) (any, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method")
		}
		return s.jwtSecret, nil
	}, jwt.WithIssuer("gitrank"), jwt.WithAudience("gitrank"))
	if err != nil {
		return 0, "", err
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok || !token.Valid {
		return 0, "", fmt.Errorf("invalid token")
	}

	tokenType, _ := claims["type"].(string)
	if tokenType != expectedType {
		return 0, "", fmt.Errorf("invalid token type")
	}

	sub, _ := claims["sub"].(string)
	login, _ := claims["login"].(string)
	userID, err := strconv.ParseInt(sub, 10, 64)
	if err != nil {
		return 0, "", fmt.Errorf("invalid user id in token")
	}

	return userID, login, nil
}

func (s *AuthService) EncryptToken(plaintext string) (string, error) {
	block, err := aes.NewCipher(s.encryptionKey)
	if err != nil {
		return "", err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}

	nonce := make([]byte, gcm.NonceSize())
	if _, err := rand.Read(nonce); err != nil {
		return "", err
	}

	ciphertext := gcm.Seal(nonce, nonce, []byte(plaintext), nil)
	return hex.EncodeToString(ciphertext), nil
}

func (s *AuthService) DecryptToken(cipherHex string) (string, error) {
	data, err := hex.DecodeString(cipherHex)
	if err != nil {
		return "", err
	}

	block, err := aes.NewCipher(s.encryptionKey)
	if err != nil {
		return "", err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}

	nonceSize := gcm.NonceSize()
	if len(data) < nonceSize {
		return "", fmt.Errorf("ciphertext too short")
	}

	nonce, ciphertext := data[:nonceSize], data[nonceSize:]
	plaintext, err := gcm.Open(nil, nonce, ciphertext, nil)
	if err != nil {
		return "", err
	}

	return string(plaintext), nil
}
