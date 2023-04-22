import { Container } from 'react-bootstrap'
import Navigation from './modules/home/Navigation'
import MainTabs from './modules/home/MainTabs'


export default () => {
  // @ts-expect-error
  const username = window.username
  return (
    <>
      <Navigation username={username} />
      <Container>
        <MainTabs />
      </Container>
    </>
  )
}
