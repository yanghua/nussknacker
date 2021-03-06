import NodeUtils from '../components/graph/NodeUtils'
import _ from 'lodash'


describe("nodes grouped", () => {
  it("should return same process when no groups found", () => {
    const process = createProcess([])
    expect(NodeUtils.nodesFromProcess(process)).toEqual(process.nodes)
  })

  it("should group nodes in line", () => {
    const process = createProcess([{id: "group1", nodes: ["node1", "node2"]}])
    expect(NodeUtils.nodesFromProcess(process)).toEqual([
      {id: "node3"},{id: "node4"}, {id: "node5"}, {id: "node6"}, {id: "node7"}, {id: "node8"},
        {id: "group1", type: "_group", nodes: [{id: "node1"}, {id: "node2"}], ids: ["node1", "node2"]}
    ])
  })

  it("should handle two groups", () => {
    const process = createProcess([{id: "group1", nodes: ["node1", "node2"]}, {id: "group2", nodes: ["node6", "node8"]}])
    expect(NodeUtils.nodesFromProcess(process)).toEqual([
      {id: "node3"},{id: "node4"}, {id: "node5"}, {id: "node7"},
        {id: "group1", type: "_group", nodes: [{id: "node1"}, {id: "node2"}], ids: ["node1", "node2"]},
        {id: "group2", type: "_group", nodes: [{id: "node6"}, {id: "node8"}], ids: ["node6", "node8"]}
    ])
  })

  it("should handle group with split", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node3", "node4", "node5", "node6"]}])
    expect(NodeUtils.nodesFromProcess(process)).toEqual([
      {id: "node1"},{id: "node2"}, {id: "node7"}, {id: "node8"},
      {id: "bigGroup", type: "_group", nodes: [{id: "node3"}, {id: "node4"}, {id: "node5"}, {id: "node6"}], ids: ["node3", "node4", "node5", "node6"]}
    ])
  })

  it("should handle group ending with split", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node1", "node2", "node3"]}])
    expect(NodeUtils.nodesFromProcess(process)).toEqual([
      {id: "node4"},{id: "node5"}, {id: "node6"}, {id: "node7"}, {id: "node8"},
      {id: "bigGroup", type: "_group", nodes: [{id: "node1"}, {id: "node2"}, {id: "node3"}], ids: ["node1", "node2", "node3"]}
    ])
  })

})


describe("edges grouped", () => {
  it("should return same process when no groups found", () => {
    const process = createProcess([])
    expect(NodeUtils.edgesFromProcess(process)).toEqual(process.edges)
  })

  it("should group nodes in line", () => {
    const process = createProcess([{id: "group1", nodes: ["node1", "node2"]}])
    expect(NodeUtils.edgesFromProcess(process)).toEqual([
      { "from": "group1", "to": "node3"},
      { "from": "node3", "to": "node4"},
      { "from": "node3", "to": "node5"},
      { "from": "node4", "to": "node6"},
      { "from": "node5", "to": "node7"},
      { "from": "node6", "to": "node8"}
    ])
  })

  it("should handle two groups", () => {
    const process = createProcess([{id: "group1", nodes: ["node1", "node2"]}, {id: "group2", nodes: ["node6", "node8"]}])
    expect(NodeUtils.edgesFromProcess(process)).toEqual([
      { "from": "group1", "to": "node3"},
      { "from": "node3", "to": "node4"},
      { "from": "node3", "to": "node5"},
      { "from": "node4", "to": "group2"},
      { "from": "node5", "to": "node7"}
    ])
  })

  it("should handle group with split", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node3", "node4", "node5", "node6"]}])
    expect(NodeUtils.edgesFromProcess(process)).toEqual([
      { "from": "node1", "to": "node2"},
      { "from": "node2", "to": "bigGroup"},
      { "from": "bigGroup", "to": "node7"},
      { "from": "bigGroup", "to": "node8"}
    ])
  })

  it("should handle group ending with split", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node1", "node2", "node3"]}])
    expect(NodeUtils.edgesFromProcess(process)).toEqual([
      { "from": "bigGroup", "to": "node4"},
      { "from": "bigGroup", "to": "node5"},
      { "from": "node4", "to": "node6"},
      { "from": "node5", "to": "node7"},
      { "from": "node6", "to": "node8"}
    ])
  })

  it("should change group after node id changes", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node1", "node2", "node3"]}])
    expect(NodeUtils.updateGroupsAfterNodeIdChange(process, "node1", "node1New").properties.additionalFields.groups).toEqual([
      {
        "id": "bigGroup",
        "nodes": ["node1New", "node2", "node3"]
      }
    ])
  })

  it("should change group after node is deleted", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node1", "node2", "node3"]}])
    expect(NodeUtils.updateGroupsAfterNodeDelete(process, "node2").properties.additionalFields.groups).toEqual([
      {
        "id": "bigGroup",
        "nodes": ["node1", "node3"]
      }
    ])
  })

  it("should edit group after group id changes", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node1", "node2", "node3"]}])
    expect(NodeUtils.editGroup(process, "bigGroup", {id: "bigGroupNew", ids: ["node4", "node5"]}).properties.additionalFields.groups).toEqual([
      {
        "id": "bigGroupNew",
        "nodes": ["node4", "node5"]
      }
    ])
  })

  it("should create group", () => {
    const process = createProcess([])
    expect(NodeUtils.createGroup(process, ["node1", "node2", "node3"]).properties.additionalFields.groups).toEqual([
      {
        "id": "node1-node2-node3",
        "nodes": ["node1", "node2", "node3"]
      }
    ])
  })

  it("should remove group", () => {
    const process = createProcess([{id: "bigGroup", nodes: ["node1", "node2", "node3"]}])
    expect(NodeUtils.ungroup(process, "bigGroup").properties.additionalFields.groups).toEqual([])
  })



})


describe("edgeType retrieved", () => {


  it("should choose unused edge type", () => {
    expect(NodeUtils.edgeType([{ from: "node1", edgeType: {type: "edge1"}}], {id: "node1", type: "Filter"}, processDefinitionData))
      .toEqual({ type: "edge2"})

  })

  it("should get edge types for node", () => {
    expect(NodeUtils.edgesForNode({id: "node1", type: "SubprocessInput", ref: {id: "sub1"}}, processDefinitionData)).toEqual({
          nodeId: { type: "SubprocessInput", id: "sub1"},
          edges: [{ type: "edge3"}]
    })
    expect(NodeUtils.edgesForNode({id: "node1", type: "Filter"}, processDefinitionData)).toEqual({
      nodeId: { type: "Filter"},
      edges: [{ type: "edge1"}, { type: "edge2"}]
    })

  })

  it("should get empty types for defaultNode", () => {
    expect(NodeUtils.edgesForNode({id: "node1", type: "Variable"}, processDefinitionData))
      .toEqual({edges: [null], canChooseNodes: false})
    expect(NodeUtils.edgesForNode({id: "node1", type: "Processor", service: {id: "sub1"}}, processDefinitionData))
      .toEqual({edges: [null], canChooseNodes: false})
  })
})

const processDefinitionData = {
  edgesForNodes: [
    {
      nodeId: { type: "Filter"},
      edges: [{ type: "edge1"}, {type: "edge2"}]
    },
    {
      nodeId: { type: "SubprocessInput", id: "sub1"},
      edges: [{ type: "edge3"}]
    },
  ]

}

const createProcess = (groups) => ({
  "properties": { additionalFields: { groups: groups || []}},
  "nodes": [
    {id: "node1"},
    {id: "node2"},
    {id: "node3"},
    {id: "node4"},
    {id: "node5"},
    {id: "node6"},
    {id: "node7"},
    {id: "node8"},

  ],
  "edges": [
    { "from": "node1", "to": "node2"},
    { "from": "node2", "to": "node3"},
    { "from": "node3", "to": "node4"},
    { "from": "node3", "to": "node5"},
    { "from": "node4", "to": "node6"},
    { "from": "node5", "to": "node7"},
    { "from": "node6", "to": "node8"}
  ]
})