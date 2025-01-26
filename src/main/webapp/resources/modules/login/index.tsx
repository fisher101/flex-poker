import { Button, Form } from "react-bootstrap"

export default () => {
    return (
      <>
        <Form className="standard-form" action="/login" method="post">
          <h3 className="mb-3 fw-normal">登陆德州扑克</h3>
          { window.location.search.includes('error') ? <p className="error-message">Invalid username/password</p> : null }
          <Form.FloatingLabel label="用户名" className="mb-1">
            <Form.Control type="text" name="username" placeholder="用户名" required autoFocus />
          </Form.FloatingLabel>
          <Form.FloatingLabel label="登陆密码" className="mb-1">
            <Form.Control type="password" name="password" placeholder="登陆密码" required />
          </Form.FloatingLabel>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <Button type="submit" variant="primary" className="mt-3 w-100 btn-lg">进入游戏</Button>
        </Form>

        <div className="row row-top-buffer">
          <div className="text-center">
            Don't have an account? <a href="/sign-up">先点这里注册!</a>
          </div>
        </div>
      </>
    )
}
