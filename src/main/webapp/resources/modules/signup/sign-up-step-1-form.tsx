import { Button, Form } from "react-bootstrap"

export default ({ error, submitFormCallback }) => {
  return (
    <Form className="standard-form" id="sign-up-step-1-form" onSubmit={submitFormCallback}>
      <h3 className="mb-3 fw-normal">注册账号</h3>
      { error ? <p className="error-message">{error}</p> : null }
      <Form.FloatingLabel label="设置用户名（英文字母）" className="mb-1">
        <Form.Control type="text" name="username" placeholder="设置用户名（英文字母）" required autoFocus />
      </Form.FloatingLabel>
      <Form.FloatingLabel label="设置密码" className="mb-1">
        <Form.Control type="password" name="password" placeholder="设置密码" required />
      </Form.FloatingLabel>
      <Form.FloatingLabel label="设置Email（随意编）" className="mb-1">
        <Form.Control type="email" name="emailAddress" placeholder="设置Email（随意编）" required />
      </Form.FloatingLabel>
      <Button type="submit" variant="primary" className="mt-3 w-100 btn-lg">确认注册</Button>
    </Form>
  )
}
