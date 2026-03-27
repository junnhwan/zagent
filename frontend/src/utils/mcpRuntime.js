export const isMcpConnected = (item) => {
  if (!item || typeof item !== 'object') {
    return false
  }
  if (typeof item.connected === 'boolean') {
    return item.connected
  }
  if (typeof item.initialized === 'boolean') {
    return item.initialized
  }
  if (typeof item.ready === 'boolean') {
    return item.ready
  }
  return false
}

export const normalizeMcpRuntimeStatus = (payload) => {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return {}
  }
  return Object.fromEntries(
    Object.entries(payload).map(([key, value]) => [
      key,
      {
        ...(value || {}),
        connected: isMcpConnected(value)
      }
    ])
  )
}
