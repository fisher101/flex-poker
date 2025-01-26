export default ({ username, email }) => {
  return (
    <>
      <h3 className="mb-3 fw-normal">确认注册</h3>
      <p>Email sent to {email} (TODO: not working yet)</p>
      <p><a href={`/sign-up-confirm?username=${username}`}>点击这里确认</a></p>
    </>
  )
}
