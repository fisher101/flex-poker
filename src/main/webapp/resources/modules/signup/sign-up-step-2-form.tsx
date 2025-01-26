import { Button, Form } from "react-bootstrap"

export default ({ error, signUpCode, submitFormCallback }) => {
  return (
    <Form className="standard-form" id="sign-up-step-2-form" onSubmit={submitFormCallback}>
      <h3 className="mb-3 fw-normal">确认注册</h3>
      {
        error
          ? <p className="error-message">{error}</p>
          : null
      }
      <Form.FloatingLabel label="用户名" className="mb-1">
        <Form.Control type="text" name="username" placeholder="用户名" required autoFocus />
      </Form.FloatingLabel>
      <input type="hidden" name="signUpCode" value={signUpCode} />
      <Button type="submit" variant="primary" className="mt-3 w-100 btn-lg">确认注册</Button>
    </Form>
  )
}
