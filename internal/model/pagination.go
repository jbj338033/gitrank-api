package model

type Cursor struct {
	S float64 `json:"s"`
	I int64   `json:"i"`
}

type ErrorResponse struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}
