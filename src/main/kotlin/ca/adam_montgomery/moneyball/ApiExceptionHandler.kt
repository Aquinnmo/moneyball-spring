package ca.adam_montgomery.moneyball

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ProblemDetail> {
        val status = HttpStatus.valueOf(ex.statusCode.value())
        return problem(status, ex.reason ?: status.reasonPhrase)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
        val detail = if (ex.name == "date") {
            "Invalid date '${ex.value}'. Use YYYY-MM-DD."
        } else {
            "Invalid value '${ex.value}' for path parameter '${ex.name}'."
        }

        return problem(HttpStatus.BAD_REQUEST, detail)
    }

    @ExceptionHandler(RestClientException::class)
    fun handleRestClientException(ex: RestClientException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_GATEWAY, "Unable to fetch data from MLB or Statcast.")

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.")

    private fun problem(status: HttpStatus, detail: String): ResponseEntity<ProblemDetail> {
        val body = ProblemDetail.forStatusAndDetail(status, detail)
        body.title = status.reasonPhrase
        return ResponseEntity.status(status).body(body)
    }
}
