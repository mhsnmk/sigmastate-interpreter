const { GroupElementObj, ValueObj } = require("sigmastate-js/main");

let pointAsn1Hex = "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";

describe("GroupElement", () => {
    it("should implement toPointHex/fromPointHex", () => {
        let ge = GroupElementObj.fromPointHex(pointAsn1Hex)
        expect(ge.toPointHex()).toEqual(pointAsn1Hex)

        let v = ValueObj.ofGroupElement(pointAsn1Hex)
        expect(v.toHex()).toEqual("07"/* GroupElement type id */ + pointAsn1Hex)
    });
});