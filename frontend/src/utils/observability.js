const STORAGE_KEY = 'za.lastRun'

const normalizeSteps = (steps) => {
  if (!Array.isArray(steps)) {
    return []
  }
  return steps.map((step, index) => ({
    sequence: step?.sequence ?? index + 1,
    clientId: step?.clientId || '',
    input: step?.input || '',
    output: step?.output || ''
  }))
}

export const saveLastObservation = (payload) => {
  const snapshot = {
    lens: payload?.lens || '',
    lensLabel: payload?.lensLabel || '',
    agentId: payload?.agentId || '',
    agentName: payload?.agentName || '',
    input: payload?.input || '',
    finalOutput: payload?.finalOutput || '',
    steps: normalizeSteps(payload?.steps),
    createdAt: new Date().toISOString()
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot))
}

export const loadLastObservation = () => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    return {
      lens: parsed.lens || '',
      lensLabel: parsed.lensLabel || '',
      agentId: parsed.agentId || '',
      agentName: parsed.agentName || '',
      input: parsed.input || '',
      finalOutput: parsed.finalOutput || '',
      steps: normalizeSteps(parsed.steps),
      createdAt: parsed.createdAt || ''
    }
  } catch {
    return null
  }
}

export const exportObservationToFile = (observation) => {
  const content = JSON.stringify(observation || {}, null, 2)
  const blob = new Blob([content], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `zagent-observation-${Date.now()}.json`
  link.click()
  URL.revokeObjectURL(url)
}
