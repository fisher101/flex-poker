import React from 'react'
import Nav from 'react-bootstrap/Nav'
import { useSelector } from 'react-redux'
import GameTab from './GameTab'
import { LinkContainer } from 'react-router-bootstrap'

export default () => {
  const openGameTabs = useSelector(state => state.openGameTabs)

  return (
    <Nav variant="tabs">
      <Nav.Item>
        <LinkContainer to="/">
          <Nav.Link>Lobby</Nav.Link>
        </LinkContainer>
      </Nav.Item>
      {openGameTabs.map((openGameTab, index) => <GameTab key={index} openGameTab={openGameTab} />)}
    </Nav>
  )
}
